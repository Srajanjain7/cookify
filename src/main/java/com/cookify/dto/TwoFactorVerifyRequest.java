package com.cookify.dto;

public record TwoFactorVerifyRequest(String identifier, String code, boolean rememberMe) {
}
