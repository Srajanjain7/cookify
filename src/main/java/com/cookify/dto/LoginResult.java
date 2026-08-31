package com.cookify.dto;

/** status: "SUCCESS" or "TWO_FACTOR_REQUIRED". */
public record LoginResult(String status, String message, UserSummaryResponse user) {
}
