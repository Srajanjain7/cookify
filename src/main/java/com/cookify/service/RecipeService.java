package com.cookify.service;

import com.cookify.dto.RecipeCreateRequest;
import com.cookify.dto.RecipeUpdateRequest;
import com.cookify.exception.ApiException;
import com.cookify.model.User;
import com.cookify.model.recipe.NonVegRecipe;
import com.cookify.model.recipe.Recipe;
import com.cookify.model.recipe.VegRecipe;
import com.cookify.repository.RatingRepository;
import com.cookify.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Recipe Upload / Edit, following the assignment's Recipe Upload pseudocode. */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RatingRepository ratingRepository;
    private final FileStorageService fileStorageService;

    public RecipeService(RecipeRepository recipeRepository,
                          RatingRepository ratingRepository,
                          FileStorageService fileStorageService) {
        this.recipeRepository = recipeRepository;
        this.ratingRepository = ratingRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public Recipe createRecipe(User creator, RecipeCreateRequest request, MultipartFile image, MultipartFile video) {
        Recipe recipe = switch (request.dietType().toUpperCase()) {
            case "VEG" -> new VegRecipe();
            case "NON_VEG" -> new NonVegRecipe();
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Please select Veg or Non-Veg");
        };

        recipe.setCreator(creator);
        applyFields(recipe, request.recipeName(), request.ingredients(), request.method(),
                request.dietaryTag(), request.cookingTimeMinutes(), request.calories(), request.protein(),
                request.cost(), request.speedRating(), request.difficultyRating(),
                request.cuisineRegion(), request.foodType());

        if (image != null && !image.isEmpty()) {
            recipe.setImagePath(fileStorageService.storeImage(image, "recipe-images"));
        }
        if (video != null && !video.isEmpty()) {
            recipe.setVideoUrl(fileStorageService.storeVideo(video, "recipe-videos"));
        }

        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe updateRecipe(User user, Long recipeId, RecipeUpdateRequest request, MultipartFile image, MultipartFile video) {
        Recipe recipe = getRecipeOrThrow(recipeId);
        if (!recipe.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only edit your own recipes");
        }

        applyFields(recipe, request.recipeName(), request.ingredients(), request.method(),
                request.dietaryTag(), request.cookingTimeMinutes(), request.calories(), request.protein(),
                request.cost(), request.speedRating(), request.difficultyRating(),
                request.cuisineRegion(), request.foodType());

        if (image != null && !image.isEmpty()) {
            recipe.setImagePath(fileStorageService.storeImage(image, "recipe-images"));
        }
        if (video != null && !video.isEmpty()) {
            recipe.setVideoUrl(fileStorageService.storeVideo(video, "recipe-videos"));
        }

        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe viewRecipe(Long recipeId) {
        Recipe recipe = getRecipeOrThrow(recipeId);
        recipe.setViews(recipe.getViews() + 1);
        return recipeRepository.save(recipe);
    }

    public List<Recipe> listRecipes() {
        return recipeRepository.findAllByOrderByViewsDescUploadDateDesc();
    }

    public double averageRating(Long recipeId) {
        Double avg = ratingRepository.findAverageScoreByRecipeId(recipeId);
        return avg == null ? 0.0 : avg;
    }

    public long ratingCount(Long recipeId) {
        return ratingRepository.countByRecipeId(recipeId);
    }

    private Recipe getRecipeOrThrow(Long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipe not found"));
    }

    private void applyFields(Recipe recipe, String recipeName, String ingredients, String method,
                              com.cookify.model.recipe.DietaryTag dietaryTag, Integer cookingTimeMinutes,
                              Integer calories, Integer protein, Double cost, Integer speedRating,
                              Integer difficultyRating, String cuisineRegion, String foodType) {
        recipe.setRecipeName(recipeName);
        recipe.setIngredients(ingredients);
        recipe.setMethod(method);
        recipe.setDietaryTag(dietaryTag);
        recipe.setCookingTimeMinutes(cookingTimeMinutes);
        recipe.setCalories(calories);
        recipe.setProtein(protein);
        recipe.setCost(cost);
        recipe.setSpeedRating(speedRating);
        recipe.setDifficultyRating(difficultyRating);
        recipe.setCuisineRegion(cuisineRegion);
        recipe.setFoodType(foodType);
    }
}
