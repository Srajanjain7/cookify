package com.cookify.dto;

import com.cookify.model.recipe.DietaryTag;
import jakarta.validation.constraints.NotBlank;

/**
 * No dietType field -- Veg/Non-Veg is a Java subclass under JPA
 * single-table inheritance and can't be changed in place after
 * creation. See DESIGN-DEVIATIONS.md.
 */
public record RecipeUpdateRequest(
        @NotBlank(message = "Error: All fields must be filled") String recipeName,
        @NotBlank(message = "Error: All fields must be filled") String ingredients,
        @NotBlank(message = "Error: All fields must be filled") String method,
        DietaryTag dietaryTag,
        Integer cookingTimeMinutes,
        Integer calories,
        Integer protein,
        Double cost,
        Integer speedRating,
        Integer difficultyRating,
        String cuisineRegion,
        String foodType,
        String requiredEquipment
) {
}
