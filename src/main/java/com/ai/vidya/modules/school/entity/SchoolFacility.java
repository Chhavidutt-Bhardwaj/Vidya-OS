package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.FacilityType;
import jakarta.persistence.*;
import lombok.*;

/**
 * A facility / infrastructure item available at the school.
 * e.g. Library, Swimming Pool, Smart Classrooms, Transport.
 *
 * Many-to-one with School.
 */
@Entity
@Table(
    name = "school_facilities",
    indexes = {
        @Index(name = "idx_facility_school_id", columnList = "school_id"),
        @Index(name = "idx_facility_type",      columnList = "facility_type")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolFacility extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 50)
    private FacilityType facilityType;

    /** Optional extra details e.g. "Olympic-size pool", "25 air-conditioned buses" */
    @Column(length = 500)
    private String description;

    /** Whether this facility is currently available / operational */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;
}