package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.EditProfileRequest;
import com.cookify.dto.UserSummaryResponse;
import com.cookify.model.User;
import com.cookify.security.CookifyUserDetails;
import com.cookify.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Requires an authenticated session -- SecurityConfig guards everything under /api/** except /api/auth/**. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserSummaryResponse> me(@AuthenticationPrincipal CookifyUserDetails principal) {
        return ApiResponse.ok("OK", UserSummaryResponse.from(principal.getUser()));
    }

    @PutMapping
    public ApiResponse<UserSummaryResponse> editProfile(@AuthenticationPrincipal CookifyUserDetails principal,
                                                          @RequestBody EditProfileRequest request) {
        User updated = userService.editProfile(principal.getUser(), request);
        return ApiResponse.ok("Profile updated", UserSummaryResponse.from(updated));
    }

    @PostMapping("/picture")
    public ApiResponse<UserSummaryResponse> uploadPicture(@AuthenticationPrincipal CookifyUserDetails principal,
                                                            @RequestParam("file") MultipartFile file) {
        User updated = userService.updateProfilePicture(principal.getUser(), file);
        return ApiResponse.ok("Profile picture updated", UserSummaryResponse.from(updated));
    }
}
