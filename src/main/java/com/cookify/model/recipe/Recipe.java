package com.cookify.model.recipe;

import com.cookify.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Corresponds to the UML "class Recipe" (drawn in italics = abstract),
 * with searchRecipe/showRecipe as service-layer behavior (Phase 4+) and
 * dietary categorization driven here by subclass polymorphism, per the
 * assignment's "OOP for recipe categorization (inheritance, classes,
 * polymorphism)" requirement.
 *
 * Recipe_Category from the source tables is folded in here rather than
 * kept as a separate table: it shares Recipe's primary key 1:1 in the
 * ERD, so splitting it serves no relational purpose. See
 * DESIGN-DEVIATIONS.md.
 */
@Entity
@Table(name = "recipes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "diet_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Lob
    @Column(nullable = false)
    private String ingredients;

    @Lob
    @Column(name = "recipe_method", nullable = false)
    private String method;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "video_url")
    private String videoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "cooking_time")
    private Integer cookingTimeMinutes;

    private Integer calories;

    private Integer protein;

    private Double cost;

    @Column(name = "speed_rating")
    private Integer speedRating;

    @Column(name = "difficulty_rating")
    private Integer difficultyRating;

    @Column(name = "cuisine_region")
    private String cuisineRegion;

    @Column(name = "food_type")
    private String foodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_tag")
    private DietaryTag dietaryTag;

    @Column(nullable = false)
    private long views = 0;

    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();

    /** Human-readable diet label, overridden per subclass. */
    public abstract String getDietaryLabel();

    /** Whether this recipe satisfies a veg-only search/preference. */
    public abstract boolean matchesDietPreference(boolean vegOnlyPreference);
}
