package com.cookify.dto;

import com.cookify.model.recipe.Recipe;

import java.time.LocalDateTime;

/** Lighter-weight shape for browse/list views (matches the Explore Recipes cards). */
public record RecipeSummaryResponse(
        Long id,
        String recipeName,
        String dietaryLabel,
        String imagePath,
        Integer cookingTimeMinutes,
        Double cost,
        Integer speedRating,
        Integer difficultyRating,
        long views,
        LocalDateTime uploadDate,
        String creatorUsername,
        double averageRating,
        long ratingCount
) {
    public static RecipeSummaryResponse from(Recipe recipe, double averageRating, long ratingCount) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getRecipeName(),
                recipe.getDietaryLabel(),
                recipe.getImagePath(),
                recipe.getCookingTimeMinutes(),
                recipe.getCost(),
                recipe.getSpeedRating(),
                recipe.getDifficultyRating(),
                recipe.getViews(),
                recipe.getUploadDate(),
                recipe.getCreator().getUsername(),
                averageRating,
                ratingCount
        );
    }
}
