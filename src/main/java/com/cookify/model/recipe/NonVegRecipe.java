package com.cookify.model.recipe;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("NON_VEG")
@Getter
@Setter
@NoArgsConstructor
public class NonVegRecipe extends Recipe {

    @Override
    public String getDietaryLabel() {
        return "Non-Vegetarian";
    }

    @Override
    public boolean matchesDietPreference(boolean vegOnlyPreference) {
        return !vegOnlyPreference;
    }
}
