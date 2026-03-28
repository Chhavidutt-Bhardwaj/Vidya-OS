package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.ExamType;
import com.ai.vidya.modules.school.entity.SchoolGradeRange;
import com.ai.vidya.modules.school.entity.SchoolTerm;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Defines an examination window for a specific term and grade range.
 *
 * <p>Relationship tree:
 * <pre>
 *   AcademicYear → SchoolTerm → ExamSchedule (many per term)
 *   AcademicYear → SchoolGradeRange → ExamSchedule (scoped per grade segment)
 * </pre>
 *
 * <p>A single ExamSchedule row represents one exam event:
 * e.g. "Mid-Term Maths for Grade 9-10, 15-Sep to 20-Sep, 10 AM".
 * Multiple rows cover different subjects or grade ranges within the same exam window.
 *
 * <p>Rollover behaviour (when RolloverTemplate.cloneExamSchedule = true):
 * Dates are shifted by the day-delta between old and new academic year start.
 * All other fields are copied verbatim. The new rows get {@code rolledOver = true}.
 */
@Entity
@Table(
    name = "exam_schedules",
    indexes = {
        @Index(name = "idx_es_term_id",        columnList = "term_id"),
        @Index(name = "idx_es_grade_range_id",  columnList = "grade_range_id"),
        @Index(name = "idx_es_exam_type",       columnList = "exam_type"),
        @Index(name = "idx_es_from_date",       columnList = "from_date")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSchedule extends BaseEntity {

    // ── Parents ────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private SchoolTerm term;

    /**
     * Grade range this exam applies to.
     * NULL means the exam applies to the whole school
     * (e.g. a common pre-board orientation).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_range_id")
    private SchoolGradeRange gradeRange;

    // ── Exam classification ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 30)
    private ExamType examType;

    /**
     * Human-readable name shown in the timetable.
     * e.g. "Half-Yearly Exam 2025", "Unit Test 2 — Science"
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // ── Schedule window ────────────────────────────────────────────────────

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    /**
     * Reporting time for students on exam days.
     * NULL if reporting time varies by subject (handled at slot level).
     */
    @Column(name = "reporting_time")
    private LocalTime reportingTime;

    // ── Marks & rules ─────────────────────────────────────────────────────

    /** Total marks for this exam window */
    @Column(name = "total_marks")
    private Integer totalMarks;

    /** Minimum marks required to pass */
    @Column(name = "passing_marks")
    private Integer passingMarks;

    /**
     * Weight of this exam in the final grade calculation (0–100).
     * e.g. mid-term = 30, final = 70 for a 30:70 split.
     */
    @Column(name = "weightage_pct")
    private Integer weightagePct;

    // ── Flags ──────────────────────────────────────────────────────────────

    /** Whether results for this exam have been finalised and published */
    @Column(name = "results_published", nullable = false)
    @Builder.Default
    private boolean resultsPublished = false;

    /** Whether this row was cloned from the previous academic year */
    @Column(name = "is_rolled_over", nullable = false)
    @Builder.Default
    private boolean rolledOver = false;

    /** Admin notes — e.g. "Hall booking confirmed", "Board format changed" */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}