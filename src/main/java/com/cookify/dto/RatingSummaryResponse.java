package com.cookify.dto;

/** myRating is null for anonymous callers or a caller who hasn't rated yet. */
public record RatingSummaryResponse(double averageRating, long ratingCount, Integer myRating) {
}
