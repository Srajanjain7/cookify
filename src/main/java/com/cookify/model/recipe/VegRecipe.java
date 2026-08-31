package com.cookify.model.recipe;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("VEG")
@Getter
@Setter
@NoArgsConstructor
public class VegRecipe extends Recipe {

    @Override
    public String getDietaryLabel() {
        return "Vegetarian";
    }

    @Override
    public boolean matchesDietPreference(boolean vegOnlyPreference) {
        return true;
    }
}
