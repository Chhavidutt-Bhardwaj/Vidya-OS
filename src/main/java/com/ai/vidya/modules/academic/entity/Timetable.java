package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.AcademicYear;
import com.ai.vidya.modules.school.entity.SchoolGradeRange;
import com.ai.vidya.modules.school.entity.SchoolShift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for the weekly timetable of one class (grade + section) for one shift.
 *
 * <p>A timetable belongs to a specific academic year and is effective
 * for a date range within that year. This allows mid-year timetable revisions
 * without overwriting the historical record.
 *
 * <p>The actual period-by-period schedule is in {@link TimetableSlot} children.
 *
 * <p>Rollover (when RolloverTemplate.cloneTimetable = true):
 * The timetable structure (slots with subjects and periods) is cloned.
 * Teacher assignments are NOT carried over — a teacher may change class
 * between years. Admin must re-assign teachers in the new timetable.
 */
@Entity
@Table(
    name = "timetables",
    indexes = {
        @Index(name = "idx_tt_academic_year_id",  columnList = "academic_year_id"),
        @Index(name = "idx_tt_grade_range_id",    columnList = "grade_range_id"),
        @Index(name = "idx_tt_shift_id",          columnList = "shift_id"),
        @Index(name = "idx_tt_active",            columnList = "active"),
        @Index(name = "idx_tt_effective_from",    columnList = "effective_from")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Timetable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_range_id", nullable = false)
    private SchoolGradeRange gradeRange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private SchoolShift shift;

    /**
     * Section label e.g. "A", "B", "Science", "Commerce".
     * NULL means the timetable applies to the entire grade (no section split).
     */
    @Column(name = "section", length = 20)
    private String section;

    // ── Version / effectivity ──────────────────────────────────────────────

    /**
     * Date from which this timetable is effective.
     * Allows mid-year revisions: create a new Timetable with a later
     * effectiveFrom date; the old one becomes historical.
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Date until which this timetable is effective (inclusive).
     * NULL means "until the end of the academic year" or until
     * superseded by a newer timetable for the same grade+section.
     */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /**
     * Whether this is the currently active timetable for its grade+section.
     * Only one timetable per grade+section+shift should be active at a time.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_rolled_over", nullable = false)
    @Builder.Default
    private boolean rolledOver = false;

    /** Admin notes e.g. "Revised after new teacher joined" */
    @Column(name = "remarks", length = 500)
    private String remarks;

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TimetableSlot> slots = new ArrayList<>();
}