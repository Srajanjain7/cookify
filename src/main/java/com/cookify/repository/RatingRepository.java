package com.cookify.repository;

import com.cookify.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByRecipeIdAndUserId(Long recipeId, Long userId);

    @Query("select avg(r.score) from Rating r where r.recipe.id = :recipeId")
    Double findAverageScoreByRecipeId(@Param("recipeId") Long recipeId);

    long countByRecipeId(Long recipeId);
}
