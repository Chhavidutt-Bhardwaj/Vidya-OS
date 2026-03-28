package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One grade band within a {@link GradingScheme}.
 *
 * <p>e.g. minMarks=91, maxMarks=100, grade="A1", gradePoint=10.0, passing=true
 *
 * <p>The entries are non-overlapping and together must span 0–100.
 * Validation is enforced by GradingSchemeService before persistence.
 */
@Entity
@Table(
    name = "grading_scheme_entries",
    indexes = {
        @Index(name = "idx_gse_scheme_id", columnList = "grading_scheme_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingSchemeEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_scheme_id", nullable = false)
    private GradingScheme gradingScheme;

    /**
     * Minimum percentage/marks for this band (inclusive).
     * Stored as BigDecimal to support 0.5-mark precision.
     */
    @Column(name = "min_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal minMarks;

    /** Maximum percentage/marks (inclusive). */
    @Column(name = "max_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxMarks;

    /** Grade label shown on report cards e.g. "A1", "A+", "Distinction" */
    @Column(name = "grade", nullable = false, length = 10)
    private String grade;

    /**
     * Grade point on a scale defined by the school (typically 0–10 for CBSE).
     * NULL for percentage-only schemes that don't use GPA.
     */
    @Column(name = "grade_point", precision = 4, scale = 2)
    private BigDecimal gradePoint;

    /**
     * Remark printed on report cards e.g. "Outstanding", "Satisfactory", "Needs Improvement"
     */
    @Column(name = "remark", length = 100)
    private String remark;

    /**
     * Whether a student scoring in this band is considered to have passed.
     * The lowest passing band determines the pass mark.
     */
    @Column(name = "is_passing", nullable = false)
    @Builder.Default
    private boolean passing = true;

    /** Display order (lowest marks first) */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}