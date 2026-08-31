package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.LoginRequest;
import com.cookify.dto.LoginResult;
import com.cookify.dto.PasswordResetConfirmRequest;
import com.cookify.dto.PasswordResetRequestDto;
import com.cookify.dto.SignUpRequest;
import com.cookify.dto.TwoFactorVerifyRequest;
import com.cookify.dto.UserSummaryResponse;
import com.cookify.model.User;
import com.cookify.service.AuthService;
import com.cookify.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account Created Successfully", UserSummaryResponse.from(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(@RequestBody LoginRequest request,
                                                            HttpServletRequest servletRequest,
                                                            HttpServletResponse servletResponse) {
        LoginResult result = authService.login(request, servletRequest, servletResponse);
        return ResponseEntity.ok(ApiResponse.ok(result.message(), result));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<LoginResult>> verifyTwoFactor(@RequestBody TwoFactorVerifyRequest request,
                                                                      HttpServletRequest servletRequest,
                                                                      HttpServletResponse servletResponse) {
        LoginResult result = authService.verifyTwoFactor(request, servletRequest, servletResponse);
        return ResponseEntity.ok(ApiResponse.ok(result.message(), result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@RequestBody PasswordResetRequestDto request) {
        authService.requestPasswordReset(request.identifier());
        return ResponseEntity.ok(ApiResponse.ok("Reset Email Sent"));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully"));
    }
}
