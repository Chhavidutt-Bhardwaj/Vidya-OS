package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Assignment of a Subject to a SchoolClass for a given academic year.
 *
 * "Mathematics is taught in Class 10 during 2024-25"
 *
 * This is separate from SectionSubjectTeacher which maps which specific
 * teacher teaches that subject in which section.
 *
 * Many-to-one with SchoolClass.
 * Many-to-one with Subject.
 */
@Entity
@Table(
    name = "class_subjects",
    indexes = {
        @Index(name = "idx_cs_class_id",   columnList = "class_id"),
        @Index(name = "idx_cs_subject_id", columnList = "subject_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_class_subject",
            columnNames = {"class_id", "subject_id"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * Class-level override for theory periods per week.
     * If null, inherits from Subject.theoryPeriodsPerWeek.
     */
    @Column(name = "theory_periods_per_week")
    private Integer theoryPeriodsPerWeek;

    /** Class-level override for practical periods. */
    @Column(name = "practical_periods_per_week")
    private Integer practicalPeriodsPerWeek;

    /**
     * Class-level override for max theory marks.
     * e.g. Class 10 Math = 80 marks, Class 12 Math = 100 marks.
     */
    @Column(name = "max_theory_marks")
    private Integer maxTheoryMarks;

    @Column(name = "max_practical_marks")
    private Integer maxPracticalMarks;

    /**
     * Whether this subject is compulsory or optional for students in this class.
     * COMPULSORY = all students take it.
     * OPTIONAL   = students choose (e.g. elective groups).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "offering_type", nullable = false, length = 15)
    @Builder.Default
    private OfferingType offeringType = OfferingType.COMPULSORY;

    public enum OfferingType {
        COMPULSORY,
        OPTIONAL
    }

    // ── Resolved helpers (merges class-level overrides with subject defaults) ─

    public int resolvedTheoryPeriods() {
        return theoryPeriodsPerWeek != null
            ? theoryPeriodsPerWeek
            : subject.getTheoryPeriodsPerWeek() != null
                ? subject.getTheoryPeriodsPerWeek() : 0;
    }

    public int resolvedPracticalPeriods() {
        return practicalPeriodsPerWeek != null
            ? practicalPeriodsPerWeek
            : subject.getPracticalPeriodsPerWeek() != null
                ? subject.getPracticalPeriodsPerWeek() : 0;
    }

    public int resolvedMaxTheoryMarks() {
        return maxTheoryMarks != null
            ? maxTheoryMarks
            : subject.getMaxTheoryMarks() != null
                ? subject.getMaxTheoryMarks() : 100;
    }

    public int resolvedMaxPracticalMarks() {
        return maxPracticalMarks != null
            ? maxPracticalMarks
            : subject.getMaxPracticalMarks() != null
                ? subject.getMaxPracticalMarks() : 0;
    }

    public int resolvedTotalMarks() {
        return resolvedMaxTheoryMarks() + resolvedMaxPracticalMarks();
    }
}
