package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.RatingRequest;
import com.cookify.dto.RatingSummaryResponse;
import com.cookify.security.CookifyUserDetails;
import com.cookify.service.RatingService;
import com.cookify.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rating (test case 9). Requires auth via SecurityConfig. */
@RestController
@RequestMapping("/api/recipes/{recipeId}/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final RecipeService recipeService;

    public RatingController(RatingService ratingService, RecipeService recipeService) {
        this.ratingService = ratingService;
        this.recipeService = recipeService;
    }

    @PostMapping
    public ApiResponse<RatingSummaryResponse> rate(@AuthenticationPrincipal CookifyUserDetails principal,
                                                     @PathVariable Long recipeId,
                                                     @Valid @RequestBody RatingRequest request) {
        ratingService.rate(principal.getUser(), recipeId, request.score());
        RatingSummaryResponse summary = new RatingSummaryResponse(
                recipeService.averageRating(recipeId),
                recipeService.ratingCount(recipeId),
                request.score());
        return ApiResponse.ok("Rating submitted", summary);
    }
}
