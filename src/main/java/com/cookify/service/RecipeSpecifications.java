package com.cookify.service;

import com.cookify.dto.RecipeSearchCriteria;
import com.cookify.model.recipe.DietaryTag;
import com.cookify.model.recipe.NonVegRecipe;
import com.cookify.model.recipe.Recipe;
import com.cookify.model.recipe.VegRecipe;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds the DB-level filter predicates for Recipe Search (query,
 * ingredients/equipment keyword match, cooking time, calories, cost,
 * speed/difficulty, cuisine region, food type, veg/non-veg). Rating
 * (average score isn't a stored column -- see DESIGN-DEVIATIONS.md)
 * and recommendation boosting happen afterward in RecipeService.
 */
final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    /** Spring Data's Specification.where(null) is ambiguous here (two overloaded functional-interface targets); build an explicit identity predicate instead. */
    private static Specification<Recipe> identity() {
        return (root, cq, cb) -> cb.conjunction();
    }

    static Specification<Recipe> fromCriteria(RecipeSearchCriteria criteria) {
        Specification<Recipe> spec = identity();
        spec = and(spec, keyword(criteria.query()));
        spec = and(spec, dietType(criteria.dietType()));
        spec = and(spec, dietaryTag(criteria.dietaryTag()));
        spec = and(spec, maxCookingTime(criteria.maxCookingTime()));
        spec = and(spec, calorieRange(criteria.minCalories(), criteria.maxCalories()));
        spec = and(spec, costRange(criteria.minCost(), criteria.maxCost()));
        spec = and(spec, minSpeed(criteria.minSpeed()));
        spec = and(spec, difficultyRange(criteria.minDifficulty(), criteria.maxDifficulty()));
        spec = and(spec, likeField("cuisineRegion", criteria.cuisineRegion()));
        spec = and(spec, likeField("foodType", criteria.foodType()));
        return spec;
    }

    /** This Specification version's and()/or() throw on a null argument rather than tolerating it -- guard here instead. */
    private static Specification<Recipe> and(Specification<Recipe> base, Specification<Recipe> addition) {
        return addition == null ? base : base.and(addition);
    }

    private static Specification<Recipe> keyword(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String like = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("recipeName")), like),
                cb.like(cb.lower(root.get("ingredients")), like),
                cb.like(cb.lower(cb.coalesce(root.get("requiredEquipment"), "")), like)
        );
    }

    private static Specification<Recipe> dietType(String dietType) {
        if (!StringUtils.hasText(dietType)) {
            return null;
        }
        return switch (dietType.toUpperCase()) {
            case "VEG" -> (root, cq, cb) -> cb.equal(root.type(), VegRecipe.class);
            case "NON_VEG" -> (root, cq, cb) -> cb.equal(root.type(), NonVegRecipe.class);
            default -> null;
        };
    }

    private static Specification<Recipe> dietaryTag(DietaryTag tag) {
        if (tag == null) {
            return null;
        }
        return (root, cq, cb) -> cb.equal(root.get("dietaryTag"), tag);
    }

    private static Specification<Recipe> maxCookingTime(Integer max) {
        if (max == null) {
            return null;
        }
        return (root, cq, cb) -> cb.and(
                cb.isNotNull(root.get("cookingTimeMinutes")),
                cb.lessThanOrEqualTo(root.get("cookingTimeMinutes"), max));
    }

    private static Specification<Recipe> calorieRange(Integer min, Integer max) {
        Specification<Recipe> spec = identity();
        if (min != null) {
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.isNotNull(root.get("calories")),
                    cb.greaterThanOrEqualTo(root.get("calories"), min)));
        }
        if (max != null) {
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.isNotNull(root.get("calories")),
                    cb.lessThanOrEqualTo(root.get("calories"), max)));
        }
        return spec;
    }

    private static Specification<Recipe> costRange(Double min, Double max) {
        Specification<Recipe> spec = identity();
        if (min != null) {
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.isNotNull(root.get("cost")),
                    cb.greaterThanOrEqualTo(root.get("cost"), min)));
        }
        if (max != null) {
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.isNotNull(root.get("cost")),
                    cb.lessThanOrEqualTo(root.get("cost"), max)));
        }
        return spec;
    }

    private static Specification<Recipe> minSpeed(Integer min) {
        if (min == null) {
            return null;
        }
        return (root, cq, cb) -> cb.and(
                cb.isNotNull(root.get("speedRating")),
                cb.greaterThanOrEqualTo(root.get("speedRating"), min));
    }

    private static Specification<Recipe> difficultyRange(Integer min, Integer max) {
        Specification<Recipe> spec = identity();
        if (min != null) {
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.isNotNull(root.get("difficultyRating")),
                    cb.greaterThanOrEqualTo(root.get("difficultyRating"), min)));
        }
        if (max != null) {
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.isNotNull(root.get("difficultyRating")),
                    cb.lessThanOrEqualTo(root.get("difficultyRating"), max)));
        }
        return spec;
    }

    private static Specification<Recipe> likeField(String field, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String like = "%" + value.toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get(field)), like);
    }
}
