package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.HolidayType;
import com.ai.vidya.modules.school.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A single calendar event (holiday, closure, school event) within an academic year.
 *
 * <p>Relationship: Many-to-one with AcademicYear.
 * A school can have many calendar events per year; one event belongs to exactly one year.
 *
 * <p>Rollover behaviour (controlled by RolloverTemplate.cloneHolidayCalendar):
 * <ul>
 *   <li>PUBLIC_HOLIDAY rows are NOT cloned — public holidays vary year to year
 *       and should be re-seeded from a government source each year.</li>
 *   <li>SCHOOL_EVENT, PTM_DAY, MAINTENANCE_BREAK rows ARE cloned with dates
 *       shifted forward by the exact number of days between the old and new
 *       year's start dates.</li>
 *   <li>EXAM_BREAK rows are cloned only when ExamSchedule is also cloned,
 *       so the break aligns with the new exam window.</li>
 * </ul>
 *
 * <p>Working-day calculation: the attendance module iterates academic year date range
 * and subtracts rows where {@code affectsAttendance = true} and the holiday type
 * is not OPTIONAL_HOLIDAY (optional holidays don't reduce working-day count by default).
 */
@Entity
@Table(
    name = "holiday_calendars",
    indexes = {
        @Index(name = "idx_hc_academic_year_id", columnList = "academic_year_id"),
        @Index(name = "idx_hc_date",             columnList = "holiday_date"),
        @Index(name = "idx_hc_type",             columnList = "holiday_type"),
        @Index(name = "idx_hc_affects_attend",   columnList = "affects_attendance")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendar extends BaseEntity {

    // ── Parent ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    // ── Core fields ────────────────────────────────────────────────────────

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    /** Display name e.g. "Diwali", "Annual Sports Day", "Republic Day" */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 30)
    private HolidayType holidayType;

    /**
     * Whether this event reduces the working-day count used in attendance %.
     * Default true for most holidays; set false for SCHOOL_EVENT days where
     * attendance is still taken (e.g. sports day counts as present).
     */
    @Column(name = "affects_attendance", nullable = false)
    @Builder.Default
    private boolean affectsAttendance = true;

    /**
     * When true, this holiday applies to all grades in the school.
     * When false, see gradeRangeIds (stored as comma-separated UUIDs in description
     * until a join table is warranted) — or scope via the grade-specific holiday
     * extension in a future iteration.
     */
    @Column(name = "affects_all_grades", nullable = false)
    @Builder.Default
    private boolean affectsAllGrades = true;

    /**
     * Optional extra note shown to parents in the school app.
     * e.g. "School will reopen on 15 Nov. Please note revised timetable."
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this was cloned from the previous year's calendar during rollover.
     * Useful for auditing and bulk-edit UIs that want to distinguish seeded vs
     * manually added events.
     */
    @Column(name = "is_rolled_over", nullable = false)
    @Builder.Default
    private boolean rolledOver = false;
}