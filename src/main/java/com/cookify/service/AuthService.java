package com.cookify.service;

import com.cookify.dto.LoginRequest;
import com.cookify.dto.LoginResult;
import com.cookify.dto.PasswordResetConfirmRequest;
import com.cookify.dto.TwoFactorVerifyRequest;
import com.cookify.dto.UserSummaryResponse;
import com.cookify.exception.ApiException;
import com.cookify.model.AccountStatus;
import com.cookify.model.User;
import com.cookify.repository.UserRepository;
import com.cookify.security.CookifyUserDetails;
import com.cookify.security.RememberMeRequestWrapper;
import com.cookify.util.Validators;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Login / 2FA / password-reset, following the assignment's Login
 * pseudocode. Password reset requires a token emailed to the account
 * (per the pseudocode's "SEND password reset mail... Reset Email
 * Sent") rather than the Password Reset prototype's apparent
 * direct-reset-with-no-verification -- see DESIGN-DEVIATIONS.md for
 * why the insecure reading was not implemented.
 */
@Service
public class AuthService {

    private static final int TWO_FACTOR_CODE_VALIDITY_MINUTES = 5;
    private static final int PASSWORD_RESET_TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecurityContextRepository securityContextRepository;
    private final PersistentTokenBasedRememberMeServices rememberMeServices;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        MailService mailService,
                        SecurityContextRepository securityContextRepository,
                        PersistentTokenBasedRememberMeServices rememberMeServices) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.securityContextRepository = securityContextRepository;
        this.rememberMeServices = rememberMeServices;
    }

    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        if (!StringUtils.hasText(request.identifier()) || !StringUtils.hasText(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Error: Incomplete Data");
        }
        if (Validators.looksLikeEmail(request.identifier()) && !Validators.validateEmailFormat(request.identifier())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Error: Invalid Email ID");
        }

        User user = findUserByIdentifier(request.identifier())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Error: User not found"));

        rejectIfNotActive(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Error: Incorrect Password");
        }

        if (user.isTwoFactorEnabled()) {
            issueTwoFactorCode(user);
            return new LoginResult("TWO_FACTOR_REQUIRED", "A verification code has been emailed to you", null);
        }

        establishSession(user, request.rememberMe(), servletRequest, servletResponse);
        return new LoginResult("SUCCESS", "Login Success", UserSummaryResponse.from(user));
    }

    @Transactional
    public LoginResult verifyTwoFactor(TwoFactorVerifyRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        User user = findUserByIdentifier(request.identifier())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Error: User not found"));

        boolean codeMissingOrExpired = user.getTwoFactorCode() == null
                || user.getTwoFactorCodeExpiresAt() == null
                || user.getTwoFactorCodeExpiresAt().isBefore(LocalDateTime.now());

        if (codeMissingOrExpired || !passwordEncoder.matches(request.code(), user.getTwoFactorCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Error: Invalid or expired code");
        }

        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiresAt(null);
        userRepository.save(user);

        establishSession(user, request.rememberMe(), servletRequest, servletResponse);
        return new LoginResult("SUCCESS", "Login Success", UserSummaryResponse.from(user));
    }

    @Transactional
    public void requestPasswordReset(String identifier) {
        Optional<User> maybeUser = findUserByIdentifier(identifier);
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            String token = generateToken();
            user.setPasswordResetToken(passwordEncoder.encode(token));
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_VALIDITY_MINUTES));
            userRepository.save(user);
            mailService.send(user.getEmail(), "Reset your COOKify password",
                    "Your password reset code is: " + token + ". It expires in "
                            + PASSWORD_RESET_TOKEN_VALIDITY_MINUTES + " minutes.");
        }
        // Same message whether or not the account exists, to avoid leaking which identifiers are registered.
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = findUserByIdentifier(request.identifier())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Error: Invalid or expired token"));

        boolean tokenMissingOrExpired = user.getPasswordResetToken() == null
                || user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now());

        if (tokenMissingOrExpired || !passwordEncoder.matches(request.token(), user.getPasswordResetToken())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Error: Invalid or expired token");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (!Validators.validatePassword(request.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 9 characters long and contain no spaces or restricted symbols");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        return userRepository.findByUsernameOrEmailOrPhone(identifier, identifier, identifier);
    }

    private void rejectIfNotActive(User user) {
        if (user.getAccountStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Error: This account has been banned");
        }
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Error: This account is temporarily suspended");
        }
    }

    private void issueTwoFactorCode(User user) {
        String code = String.valueOf(100000 + secureRandom.nextInt(900000));
        user.setTwoFactorCode(passwordEncoder.encode(code));
        user.setTwoFactorCodeExpiresAt(LocalDateTime.now().plusMinutes(TWO_FACTOR_CODE_VALIDITY_MINUTES));
        userRepository.save(user);
        mailService.send(user.getEmail(), "Your COOKify verification code",
                "Your 6-digit code is: " + code + ". It expires in " + TWO_FACTOR_CODE_VALIDITY_MINUTES + " minutes.");
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void establishSession(User user, boolean rememberMe, HttpServletRequest request, HttpServletResponse response) {
        CookifyUserDetails principal = new CookifyUserDetails(user);
        UsernamePasswordAuthenticationToken authToken =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        if (rememberMe) {
            rememberMeServices.loginSuccess(new RememberMeRequestWrapper(request), response, authToken);
        }
    }
}
