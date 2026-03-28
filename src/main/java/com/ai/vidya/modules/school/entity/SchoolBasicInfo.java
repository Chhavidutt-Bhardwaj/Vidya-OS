package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Extended basic-info for a school — kept separate to keep the School
 * table lean and allow lazy loading of rarely-needed fields.
 *
 * One-to-one with School.
 */
@Entity
@Table(name = "school_basic_info")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolBasicInfo extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    /** Short tagline/motto shown on reports and certificates */
    @Column(length = 500)
    private String tagline;

    /** Full description / about the school */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Year the school was established */
    @Column(name = "established_year")
    private Integer establishedYear;

    /** Official founding / registration date */
    @Column(name = "founded_on")
    private LocalDate foundedOn;

    /** Name of the current principal */
    @Column(name = "principal_name", length = 150)
    private String principalName;

    /** Designation of the head (Principal / Director / Headmaster) */
    @Column(name = "principal_designation", length = 100)
    private String principalDesignation;

    /** Official school email visible to parents */
    @Column(name = "official_email", length = 255)
    private String officialEmail;

    /** Official website */
    @Column(length = 255)
    private String website;

    /** Primary phone number */
    @Column(name = "phone_primary", length = 20)
    private String phonePrimary;

    /** Secondary / alternate phone */
    @Column(name = "phone_secondary", length = 20)
    private String phoneSecondary;

    /** S3 / CDN path to the school logo */
    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    /** S3 / CDN path to the school cover / banner image */
    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    /** Registration number from state education dept */
    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    /** Trust / society name that runs the school */
    @Column(name = "trust_name", length = 255)
    private String trustName;

    /** Management type: GOVERNMENT, PRIVATE, AIDED, AUTONOMOUS */
    @Column(name = "management_type", length = 50)
    private String managementType;

    /** Whether the school is co-educational */
    @Column(name = "is_co_ed", nullable = false)
    @Builder.Default
    private boolean coEd = true;

    /** Whether the school is residential (boarding) */
    @Column(name = "is_residential", nullable = false)
    @Builder.Default
    private boolean residential = false;
}