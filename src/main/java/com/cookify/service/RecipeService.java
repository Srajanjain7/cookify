package com.cookify.service;

import com.cookify.dto.RecipeCreateRequest;
import com.cookify.dto.RecipeSearchCriteria;
import com.cookify.dto.RecipeSummaryResponse;
import com.cookify.dto.RecipeUpdateRequest;
import com.cookify.exception.ApiException;
import com.cookify.model.User;
import com.cookify.model.recipe.DietaryTag;
import com.cookify.model.recipe.NonVegRecipe;
import com.cookify.model.recipe.Recipe;
import com.cookify.model.recipe.VegRecipe;
import com.cookify.repository.RatingRepository;
import com.cookify.repository.RecipeRepository;
import com.cookify.repository.SubscriptionRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recipe Upload / Edit / Search, following the assignment's Recipe
 * Upload and Recipe Search pseudocode.
 */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RatingRepository ratingRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final FileStorageService fileStorageService;

    public RecipeService(RecipeRepository recipeRepository,
                          RatingRepository ratingRepository,
                          SubscriptionRepository subscriptionRepository,
                          FileStorageService fileStorageService) {
        this.recipeRepository = recipeRepository;
        this.ratingRepository = ratingRepository;
        this.subscriptionRepository = subscriptionRepository;
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
                request.requiredEquipment(), request.dietaryTag(), request.cookingTimeMinutes(),
                request.calories(), request.protein(), request.cost(), request.speedRating(),
                request.difficultyRating(), request.cuisineRegion(), request.foodType());

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
                request.requiredEquipment(), request.dietaryTag(), request.cookingTimeMinutes(),
                request.calories(), request.protein(), request.cost(), request.speedRating(),
                request.difficultyRating(), request.cuisineRegion(), request.foodType());

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

    /**
     * Recipe Search, per the pseudocode's "SEARCH Recipes... APPLY
     * Filters and Preferences... MATCH Recipes that meet criteria".
     * An absent/blank query is treated as "no keyword filter" rather
     * than the pseudocode's hard error -- the Browse Page prototype
     * itself supports filtering (speed/difficulty/veg toggle) with no
     * search box text typed, so rejecting that would break normal
     * browsing. See DESIGN-DEVIATIONS.md.
     */
    public List<RecipeSummaryResponse> searchRecipes(RecipeSearchCriteria criteria, User currentUserOrNull) {
        Specification<Recipe> spec = RecipeSpecifications.fromCriteria(criteria);
        List<Recipe> candidates = recipeRepository.findAll(spec);

        record Scored(Recipe recipe, double avgRating, long ratingCount) {
        }

        List<Scored> scored = candidates.stream()
                .map(r -> new Scored(r, averageRating(r.getId()), ratingCount(r.getId())))
                .filter(s -> criteria.minRating() == null || s.avgRating() >= criteria.minRating())
                .collect(Collectors.toCollection(ArrayList::new));

        String sortKey = criteria.sort() == null ? "popular" : criteria.sort();
        Comparator<Scored> comparator = switch (sortKey) {
            case "newest" -> Comparator.comparing((Scored s) -> s.recipe().getUploadDate()).reversed();
            case "topRated" -> Comparator.comparingDouble((Scored s) -> s.avgRating()).reversed()
                    .thenComparing(Comparator.comparingLong((Scored s) -> s.ratingCount()).reversed());
            default -> Comparator.comparingLong((Scored s) -> s.recipe().getViews()).reversed()
                    .thenComparing(Comparator.comparing((Scored s) -> s.recipe().getUploadDate()).reversed());
        };

        if (criteria.recommended() && currentUserOrNull != null) {
            Set<Long> followedCreatorIds = subscriptionRepository.findBySubscriberId(currentUserOrNull.getId()).stream()
                    .map(sub -> sub.getCreator().getId())
                    .collect(Collectors.toSet());
            if (!followedCreatorIds.isEmpty()) {
                Comparator<Scored> followedFirst =
                        Comparator.comparing(s -> followedCreatorIds.contains(s.recipe().getCreator().getId()) ? 0 : 1);
                comparator = followedFirst.thenComparing(comparator);
            }
        }

        scored.sort(comparator);

        return scored.stream()
                .map(s -> RecipeSummaryResponse.from(s.recipe(), s.avgRating(), s.ratingCount()))
                .toList();
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
                              String requiredEquipment, DietaryTag dietaryTag, Integer cookingTimeMinutes,
                              Integer calories, Integer protein, Double cost, Integer speedRating,
                              Integer difficultyRating, String cuisineRegion, String foodType) {
        recipe.setRecipeName(recipeName);
        recipe.setIngredients(ingredients);
        recipe.setMethod(method);
        recipe.setRequiredEquipment(requiredEquipment);
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
