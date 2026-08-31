package com.cookify.model.recipe;

/**
 * Finer-grained dietary preference than the Veg/Non-Veg class split
 * (test case 5: "eggetarian, pescetarian, jain etc."). The UML only
 * specifies two Recipe subclasses, so this stays a plain field rather
 * than further subclassing.
 */
public enum DietaryTag {
    VEGAN,
    VEGETARIAN,
    EGGETARIAN,
    PESCETARIAN,
    JAIN,
    NON_VEGETARIAN
}
