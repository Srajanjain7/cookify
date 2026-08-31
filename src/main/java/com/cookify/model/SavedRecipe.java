package com.cookify.model;

import com.cookify.model.recipe.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Backs the Profile Page's "Saved Recipes" carousel (prototype only,
 * no DB support in the source material) and replaces the source
 * table's flat "Pref_recipe_id : varchar" list with a proper relation.
 * See DESIGN-DEVIATIONS.md.
 */
@Entity
@Table(name = "saved_recipes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recipe_id"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SavedRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt = LocalDateTime.now();
}
