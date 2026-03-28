package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The grading / marks-to-grade mapping for an academic year.
 *
 * <p>Each school can define its own grading system per year.
 * The scheme contains a list of {@link GradingSchemeEntry} rows that
 * map a mark range (e.g. 90–100) to a grade label (e.g. "A+") and grade point.
 *
 * <p>A school typically has one GradingScheme per academic year,
 * but a chain may allow per-branch overrides.
 *
 * <p>Rollover: cloned verbatim — grading usually doesn't change year to year.
 * Admin can edit before activating the new year.
 *
 * <p>Examples:
 * <ul>
 *   <li>CBSE 10-point GPA: A1 (91-100, 10.0), A2 (81-90, 9.0) …</li>
 *   <li>Percentage based: Distinction ≥75%, First 60–74%, Second 45–59%</li>
 * </ul>
 */
@Entity
@Table(
    name = "grading_schemes",
    indexes = {
        @Index(name = "idx_gs_academic_year_id", columnList = "academic_year_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_gs_year_name",
            columnNames = {"academic_year_id", "name"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingScheme extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /** e.g. "CBSE 10-Point GPA 2025-26" */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Whether this is the default scheme applied across all grade ranges.
     * Only one scheme per year should be marked default.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultScheme = true;

    /** Whether pass/fail is computed as percentage (true) or fixed marks (false) */
    @Column(name = "percentage_based", nullable = false)
    @Builder.Default
    private boolean percentageBased = true;

    @Column(name = "is_rolled_over", nullable = false)
    @Builder.Default
    private boolean rolledOver = false;

    @OneToMany(mappedBy = "gradingScheme", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GradingSchemeEntry> entries = new ArrayList<>();
}