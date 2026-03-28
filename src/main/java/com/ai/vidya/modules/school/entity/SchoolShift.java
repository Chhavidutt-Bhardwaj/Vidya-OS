package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * A school shift — Morning, Afternoon, Evening.
 * Linked to an AcademicYear so shift timings can vary year to year.
 *
 * Many-to-one with AcademicYear.
 */
@Entity
@Table(
    name = "school_shifts",
    indexes = {
        @Index(name = "idx_shift_academic_year_id", columnList = "academic_year_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolShift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /** e.g. "Morning Shift", "Afternoon Shift" */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Whether this is the default/primary shift */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultShift = false;
}