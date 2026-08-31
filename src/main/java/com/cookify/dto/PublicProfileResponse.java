package com.cookify.dto;

import com.cookify.model.User;

/** subscribedByMe is null for anonymous viewers, true/false once a viewer is authenticated. */
public record PublicProfileResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String bio,
        String profilePicturePath,
        long followerCount,
        long followingCount,
        long uploadedRecipeCount,
        Boolean subscribedByMe
) {
    public static PublicProfileResponse from(User user, long followerCount, long followingCount,
                                              long uploadedRecipeCount, Boolean subscribedByMe) {
        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePicturePath(),
                followerCount,
                followingCount,
                uploadedRecipeCount,
                subscribedByMe
        );
    }
}
