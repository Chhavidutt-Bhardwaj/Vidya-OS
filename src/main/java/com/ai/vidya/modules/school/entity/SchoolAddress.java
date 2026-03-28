package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.AddressEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Physical address of one school campus.
 * Extends the shared AddressEntity (address_line1/2, city, district,
 * state, pincode, lat/lng) and adds a school-specific foreign key.
 *
 * One-to-one with School.
 */
@Entity
@Table(
    name = "school_addresses",
    indexes = {
        @Index(name = "idx_school_addr_pincode", columnList = "pincode"),
        @Index(name = "idx_school_addr_city",    columnList = "city"),
        @Index(name = "idx_school_addr_state",   columnList = "state")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolAddress extends AddressEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    /**
     * Free-text directions to the school gate
     * e.g. "Opposite City Mall, Near Metro Station"
     */
    @Column(name = "directions", columnDefinition = "TEXT")
    private String directions;

    /** Google Maps / Apple Maps short link (if stored) */
    @Column(name = "map_link", length = 512)
    private String mapLink;

    /** Google Place ID for rich map integration */
    @Column(name = "google_place_id", length = 100)
    private String googlePlaceId;
}