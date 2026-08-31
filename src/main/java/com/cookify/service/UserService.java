package com.cookify.service;

import com.cookify.dto.EditProfileRequest;
import com.cookify.dto.SignUpRequest;
import com.cookify.exception.ApiException;
import com.cookify.model.User;
import com.cookify.model.UserPreference;
import com.cookify.repository.UserPreferenceRepository;
import com.cookify.repository.UserRepository;
import com.cookify.util.Validators;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Sign Up + Edit Profile, following the assignment's Sign Up pseudocode. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository,
                        UserPreferenceRepository userPreferenceRepository,
                        PasswordEncoder passwordEncoder,
                        FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public User signUp(SignUpRequest request) {
        if (!isUserIDUnique(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "This Username is taken, try again");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username or Email ID already in use!");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (!Validators.validatePassword(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 9 characters long and contain no spaces or restricted symbols");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBio(request.bio());
        user = userRepository.save(user);

        UserPreference preference = new UserPreference();
        preference.setUser(user);
        preference.setVegOnly(false);
        userPreferenceRepository.save(preference);
        user.setPreference(preference);

        return user;
    }

    public User editProfile(User user, EditProfileRequest request) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.twoFactorEnabled() != null) {
            user.setTwoFactorEnabled(request.twoFactorEnabled());
        }
        return userRepository.save(user);
    }

    public User updateProfilePicture(User user, MultipartFile file) {
        String path = fileStorageService.storeImage(file, "profile-pictures");
        user.setProfilePicturePath(path);
        return userRepository.save(user);
    }

    /** isUserIDUnique from the Sign Up pseudocode. */
    public boolean isUserIDUnique(String username) {
        return !userRepository.existsByUsername(username);
    }
}
