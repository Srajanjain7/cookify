package com.cookify.service;

import com.cookify.exception.ApiException;
import com.cookify.model.Rating;
import com.cookify.model.User;
import com.cookify.model.recipe.Recipe;
import com.cookify.repository.RatingRepository;
import com.cookify.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Rating, following the assignment's Rating pseudocode; re-rating updates the existing score. */
@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RecipeRepository recipeRepository;
    private final MailService mailService;

    public RatingService(RatingRepository ratingRepository, RecipeRepository recipeRepository, MailService mailService) {
        this.ratingRepository = ratingRepository;
        this.recipeRepository = recipeRepository;
        this.mailService = mailService;
    }

    @Transactional
    public Rating rate(User user, Long recipeId, int score) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipe not found"));

        Optional<Rating> existing = ratingRepository.findByRecipeIdAndUserId(recipeId, user.getId());
        Rating rating = existing.orElseGet(Rating::new);
        boolean isNew = existing.isEmpty();
        rating.setRecipe(recipe);
        rating.setUser(user);
        rating.setScore(score);
        rating = ratingRepository.save(rating);

        if (isNew && !recipe.getCreator().getId().equals(user.getId())) {
            mailService.send(recipe.getCreator().getEmail(), "New rating on your recipe",
                    user.getUsername() + " rated \"" + recipe.getRecipeName() + "\" " + score + "/5.");
        }

        return rating;
    }

    public Optional<Integer> myRating(Long recipeId, Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return ratingRepository.findByRecipeIdAndUserId(recipeId, userId).map(Rating::getScore);
    }
}
