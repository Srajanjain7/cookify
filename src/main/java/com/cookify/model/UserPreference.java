package com.cookify.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shares its primary key with User (1:1, same PK), matching the ERD's
 * User_Preference entity. preferred/uploaded recipe lists from the source
 * table are modeled as proper relations elsewhere rather than varchar
 * lists here — see DESIGN-DEVIATIONS.md.
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserPreference {

    @Id
    @EqualsAndHashCode.Include
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "veg_only", nullable = false)
    private boolean vegOnly = false;

    private Integer calories;

    private Integer protein;
}
