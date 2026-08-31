package com.cookify.dto;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Please confirm your password") String confirmPassword,
        String firstName,
        String lastName,
        String bio
) {
}
