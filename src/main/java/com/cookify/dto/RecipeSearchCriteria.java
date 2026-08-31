package com.cookify.dto;

import com.cookify.model.recipe.DietaryTag;

/**
 * All fields optional -- an all-null criteria means "browse everything".
 * sort: "popular" (default, views then upload date), "newest", or "topRated".
 * recommended: boost recipes from creators the current user follows
 * (test case 13); ignored for anonymous requests.
 */
public record RecipeSearchCriteria(
        String query,
        String dietType,
        DietaryTag dietaryTag,
        Integer maxCookingTime,
        Integer minCalories,
        Integer maxCalories,
        Double minCost,
        Double maxCost,
        Integer minSpeed,
        Integer minDifficulty,
        Integer maxDifficulty,
        String cuisineRegion,
        String foodType,
        Double minRating,
        String sort,
        boolean recommended
) {
}
