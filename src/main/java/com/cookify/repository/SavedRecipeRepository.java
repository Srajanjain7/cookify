package com.cookify.repository;

import com.cookify.model.SavedRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {
    List<SavedRecipe> findByUserId(Long userId);
    Optional<SavedRecipe> findByUserIdAndRecipeId(Long userId, Long recipeId);
}
