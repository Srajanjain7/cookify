package com.cookify.repository;

import com.cookify.model.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByRecipeNameContainingIgnoreCase(String keyword);
    List<Recipe> findByCreatorId(Long creatorId);
    List<Recipe> findAllByOrderByViewsDescUploadDateDesc();
}
