package com.cookify.dto;

public record EditProfileRequest(
        String firstName,
        String lastName,
        Integer age,
        String gender,
        String bio,
        Boolean twoFactorEnabled
) {
}
