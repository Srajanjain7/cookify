package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.RecipeCreateRequest;
import com.cookify.dto.RecipeResponse;
import com.cookify.dto.RecipeSearchCriteria;
import com.cookify.dto.RecipeSummaryResponse;
import com.cookify.dto.RecipeUpdateRequest;
import com.cookify.model.recipe.DietaryTag;
import com.cookify.model.recipe.Recipe;
import com.cookify.security.CookifyUserDetails;
import com.cookify.service.RatingService;
import com.cookify.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final RatingService ratingService;

    public RecipeController(RecipeService recipeService, RatingService ratingService) {
        this.recipeService = recipeService;
        this.ratingService = ratingService;
    }

    /**
     * Recipe Search + filter-based browsing (test cases 4-6). Public --
     * browsing recipes needs no login; POST/PUT require auth via
     * SecurityConfig. `recommended=true` is silently ignored for
     * anonymous callers.
     */
    @GetMapping
    public ApiResponse<List<RecipeSummaryResponse>> search(@AuthenticationPrincipal CookifyUserDetails principal,
                                                             @RequestParam(required = false) String query,
                                                             @RequestParam(required = false) String dietType,
                                                             @RequestParam(required = false) DietaryTag dietaryTag,
                                                             @RequestParam(required = false) Integer maxCookingTime,
                                                             @RequestParam(required = false) Integer minCalories,
                                                             @RequestParam(required = false) Integer maxCalories,
                                                             @RequestParam(required = false) Double minCost,
                                                             @RequestParam(required = false) Double maxCost,
                                                             @RequestParam(required = false) Integer minSpeed,
                                                             @RequestParam(required = false) Integer minDifficulty,
                                                             @RequestParam(required = false) Integer maxDifficulty,
                                                             @RequestParam(required = false) String cuisineRegion,
                                                             @RequestParam(required = false) String foodType,
                                                             @RequestParam(required = false) Double minRating,
                                                             @RequestParam(required = false) String sort,
                                                             @RequestParam(defaultValue = "false") boolean recommended) {
        RecipeSearchCriteria criteria = new RecipeSearchCriteria(query, dietType, dietaryTag, maxCookingTime,
                minCalories, maxCalories, minCost, maxCost, minSpeed, minDifficulty, maxDifficulty,
                cuisineRegion, foodType, minRating, sort, recommended);
        List<RecipeSummaryResponse> recipes = recipeService.searchRecipes(criteria,
                principal == null ? null : principal.getUser());
        return ApiResponse.ok("OK", recipes);
    }

    @GetMapping("/{id}")
    public ApiResponse<RecipeResponse> get(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable Long id) {
        Recipe recipe = recipeService.viewRecipe(id);
        Long userId = principal == null ? null : principal.getUser().getId();
        Integer myRating = ratingService.myRating(id, userId).orElse(null);
        return ApiResponse.ok("OK",
                RecipeResponse.from(recipe, recipeService.averageRating(id), recipeService.ratingCount(id), myRating));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<RecipeResponse> create(@AuthenticationPrincipal CookifyUserDetails principal,
                                               @Valid @RequestPart("recipe") RecipeCreateRequest request,
                                               @RequestPart(value = "image", required = false) MultipartFile image,
                                               @RequestPart(value = "video", required = false) MultipartFile video) {
        Recipe recipe = recipeService.createRecipe(principal.getUser(), request, image, video);
        return ApiResponse.ok("Recipe Uploaded Successfully",
                RecipeResponse.from(recipe, recipeService.averageRating(recipe.getId()), recipeService.ratingCount(recipe.getId())));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponse<RecipeResponse> update(@AuthenticationPrincipal CookifyUserDetails principal,
                                               @PathVariable Long id,
                                               @Valid @RequestPart("recipe") RecipeUpdateRequest request,
                                               @RequestPart(value = "image", required = false) MultipartFile image,
                                               @RequestPart(value = "video", required = false) MultipartFile video) {
        Recipe recipe = recipeService.updateRecipe(principal.getUser(), id, request, image, video);
        return ApiResponse.ok("Recipe updated",
                RecipeResponse.from(recipe, recipeService.averageRating(id), recipeService.ratingCount(id)));
    }
}
