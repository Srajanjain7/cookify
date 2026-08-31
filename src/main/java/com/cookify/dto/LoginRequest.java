package com.cookify.dto;

/** identifier = Username / Email / Phone Number, per the Login prototype. */
public record LoginRequest(String identifier, String password, boolean rememberMe) {
}
