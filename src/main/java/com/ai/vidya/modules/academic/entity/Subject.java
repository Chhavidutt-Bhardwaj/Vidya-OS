package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.SubjectType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A subject offered by a school.
 * e.g. Mathematics, Physics, Hindi, Physical Education
 *
 * Subjects are defined at school level and then assigned to specific
 * classes via ClassSubject. One Subject row can be taught across
 * multiple classes in the same or different academic years.
 */
@Entity
@Table(
    name = "subjects",
    indexes = {
        @Index(name = "idx_subject_school_id",  columnList = "school_id"),
        @Index(name = "idx_subject_type",        columnList = "subject_type"),
        @Index(name = "idx_subject_code",        columnList = "school_id, code")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_subject_school_code",
            columnNames = {"school_id", "code"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends BaseEntity {

    /** The school that owns this subject definition */
    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    /**
     * Short unique code within the school.
     * e.g. "MATH", "PHY", "ENG-A", "PE"
     */
    @Column(nullable = false, length = 20)
    private String code;

    /** Full display name e.g. "Mathematics", "English Language" */
    @Column(nullable = false, length = 100)
    private String name;

    /** Optional short name for timetable cells e.g. "Math", "Eng" */
    @Column(name = "short_name", length = 30)
    private String shortName;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    /**
     * Theory credit hours per week (used for timetable generation).
     * e.g. 5 for Mathematics
     */
    @Column(name = "theory_periods_per_week")
    private Integer theoryPeriodsPerWeek;

    /**
     * Practical/lab periods per week (if applicable).
     * e.g. 2 for Science labs
     */
    @Column(name = "practical_periods_per_week")
    @Builder.Default
    private Integer practicalPeriodsPerWeek = 0;

    /**
     * Max marks for theory component.
     * Drives mark-entry validation and report card totals.
     */
    @Column(name = "max_theory_marks")
    private Integer maxTheoryMarks;

    /** Max marks for practical component. Null if no practicals. */
    @Column(name = "max_practical_marks")
    private Integer maxPracticalMarks;

    /** Whether marks are graded (A/B/C) vs numeric */
    @Column(name = "is_graded", nullable = false)
    @Builder.Default
    private boolean graded = false;

    /** Whether this subject is active and can be assigned to classes */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Board override — some subjects follow a different board than the school default */
    @Column(name = "board_override", length = 30)
    private String boardOverride;

    /** Color hex for timetable display e.g. "#4A90D9" */
    @Column(name = "color_hex", length = 10)
    private String colorHex;
}
