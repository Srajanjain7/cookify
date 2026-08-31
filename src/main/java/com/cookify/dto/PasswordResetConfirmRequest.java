package com.cookify.dto;

public record PasswordResetConfirmRequest(
        String identifier,
        String token,
        String newPassword,
        String confirmNewPassword
) {
}
