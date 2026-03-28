package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A term / semester within an academic year.
 * e.g. Term 1 (Apr–Sep), Term 2 (Oct–Mar)
 *      or Q1, Q2, Q3, Q4 for quarterly schools.
 *
 * Many-to-one with AcademicYear.
 */
@Entity
@Table(
    name = "school_terms",
    indexes = {
        @Index(name = "idx_term_academic_year_id", columnList = "academic_year_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolTerm extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /** e.g. "Term 1", "Semester 1", "Quarter 1" */
    @Column(nullable = false, length = 50)
    private String name;

    /** Sequence for ordering: 1, 2, 3 … */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Whether this term's marks are locked from further editing */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = false;
}