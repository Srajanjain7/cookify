package com.cookify.dto;

import com.cookify.model.recipe.DietaryTag;
import jakarta.validation.constraints.NotBlank;

/** dietType: "VEG" or "NON_VEG" -- chooses the Recipe subclass and is immutable after creation. */
public record RecipeCreateRequest(
        @NotBlank(message = "Error: All fields must be filled") String recipeName,
        @NotBlank(message = "Error: All fields must be filled") String ingredients,
        @NotBlank(message = "Error: All fields must be filled") String method,
        @NotBlank(message = "Please select Veg or Non-Veg") String dietType,
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
