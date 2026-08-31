package com.cookify.repository;

import com.cookify.model.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
    List<Recipe> findByRecipeNameContainingIgnoreCase(String keyword);
    List<Recipe> findByCreatorId(Long creatorId);
    long countByCreatorId(Long creatorId);
    List<Recipe> findAllByOrderByViewsDescUploadDateDesc();
}
