package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.AcademicYear;
import com.ai.vidya.modules.school.entity.SchoolGradeRange;
import jakarta.persistence.*;
import lombok.*;

/**
 * Maps a subject to a grade range for a specific academic year.
 *
 * <p>One row = one subject offered to one grade segment in one academic year.
 * e.g. "Mathematics → Secondary (9-10) → 2025-26"
 *
 * <p>This table drives timetable slot creation (which subjects can be scheduled),
 * exam schedule generation (which subjects need exam slots), and report card
 * column headers.
 *
 * <p>Rollover: cloned verbatim. New subjects can be added / removed before
 * the new year is activated. Removed subjects cascade-delete their timetable slots.
 */
@Entity
@Table(
    name = "subject_mappings",
    indexes = {
        @Index(name = "idx_sm_academic_year_id",  columnList = "academic_year_id"),
        @Index(name = "idx_sm_grade_range_id",    columnList = "grade_range_id"),
        @Index(name = "idx_sm_subject_code",      columnList = "subject_code")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_sm_year_grade_code",
            columnNames = {"academic_year_id", "grade_range_id", "subject_code"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMapping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_range_id", nullable = false)
    private SchoolGradeRange gradeRange;

    // ── Subject identity ───────────────────────────────────────────────────

    /**
     * Short code used in timetable, report cards, and exam schedules.
     * e.g. "MATH", "PHY", "ENG-LANG", "HIN"
     * Unique within the year+grade constraint above.
     */
    @Column(name = "subject_code", nullable = false, length = 30)
    private String subjectCode;

    /** Full display name e.g. "Mathematics", "Physics", "English Language" */
    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    /**
     * Optional group / stream — useful for elective streams in Grade 11-12.
     * e.g. "Science", "Commerce", "Humanities", "Vocational"
     */
    @Column(name = "stream", length = 50)
    private String stream;

    // ── Configuration ──────────────────────────────────────────────────────

    /**
     * Periods per week allocated to this subject in the timetable.
     * The TimetableService uses this to validate that the generated
     * timetable satisfies each subject's weekly period requirement.
     */
    @Column(name = "periods_per_week", nullable = false)
    @Builder.Default
    private int periodsPerWeek = 5;

    /** Whether this subject is assessed in term exams (some activity subjects are not) */
    @Column(name = "is_examinable", nullable = false)
    @Builder.Default
    private boolean examinable = true;

    /**
     * Whether this subject is elective (student chooses from a pool)
     * vs core (mandatory for all students in the grade range).
     */
    @Column(name = "is_elective", nullable = false)
    @Builder.Default
    private boolean elective = false;

    @Column(name = "is_rolled_over", nullable = false)
    @Builder.Default
    private boolean rolledOver = false;

    /** UI order in timetable and report card columns */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}