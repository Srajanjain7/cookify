package com.cookify.dto;

import com.cookify.model.recipe.DietaryTag;
import com.cookify.model.recipe.Recipe;

import java.time.LocalDateTime;

public record RecipeResponse(
        Long id,
        String recipeName,
        String ingredients,
        String method,
        String requiredEquipment,
        String imagePath,
        String videoUrl,
        String dietaryLabel,
        DietaryTag dietaryTag,
        Integer cookingTimeMinutes,
        Integer calories,
        Integer protein,
        Double cost,
        Integer speedRating,
        Integer difficultyRating,
        String cuisineRegion,
        String foodType,
        long views,
        LocalDateTime uploadDate,
        Long creatorId,
        String creatorUsername,
        double averageRating,
        long ratingCount,
        Integer myRating
) {
    public static RecipeResponse from(Recipe recipe, double averageRating, long ratingCount) {
        return from(recipe, averageRating, ratingCount, null);
    }

    public static RecipeResponse from(Recipe recipe, double averageRating, long ratingCount, Integer myRating) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getRecipeName(),
                recipe.getIngredients(),
                recipe.getMethod(),
                recipe.getRequiredEquipment(),
                recipe.getImagePath(),
                recipe.getVideoUrl(),
                recipe.getDietaryLabel(),
                recipe.getDietaryTag(),
                recipe.getCookingTimeMinutes(),
                recipe.getCalories(),
                recipe.getProtein(),
                recipe.getCost(),
                recipe.getSpeedRating(),
                recipe.getDifficultyRating(),
                recipe.getCuisineRegion(),
                recipe.getFoodType(),
                recipe.getViews(),
                recipe.getUploadDate(),
                recipe.getCreator().getId(),
                recipe.getCreator().getUsername(),
                averageRating,
                ratingCount,
                myRating
        );
    }
}
