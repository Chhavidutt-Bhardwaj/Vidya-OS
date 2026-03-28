package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Defines the grade/class range a school offers.
 * e.g. "Pre-Primary: Nursery–KG2" or "Secondary: 9–12".
 *
 * A school can have multiple grade ranges (segments).
 *
 * Many-to-one with School.
 */
@Entity
@Table(
    name = "school_grade_ranges",
    indexes = {
        @Index(name = "idx_grade_range_school_id", columnList = "school_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolGradeRange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /**
     * Segment label e.g. "Pre-Primary", "Primary", "Middle", "Secondary", "Senior Secondary"
     */
    @Column(name = "segment_name", nullable = false, length = 100)
    private String segmentName;

    /**
     * Start grade/class label e.g. "Nursery", "1", "6"
     */
    @Column(name = "from_grade", nullable = false, length = 20)
    private String fromGrade;

    /**
     * End grade/class label e.g. "KG2", "5", "8", "12"
     */
    @Column(name = "to_grade", nullable = false, length = 20)
    private String toGrade;

    /** Numeric start for sorting — maps fromGrade to an int */
    @Column(name = "from_grade_order", nullable = false)
    private int fromGradeOrder;

    /** Numeric end for sorting */
    @Column(name = "to_grade_order", nullable = false)
    private int toGradeOrder;

    /** e.g. "CBSE", "State Board" — if different per segment */
    @Column(name = "board_override", length = 30)
    private String boardOverride;
}