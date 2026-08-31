package com.cookify.dto;

import com.cookify.model.User;

public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String bio,
        String profilePicturePath,
        boolean twoFactorEnabled
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePicturePath(),
                user.isTwoFactorEnabled()
        );
    }
}
