package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.School;
import jakarta.persistence.*;
import lombok.*;

/**
 * Controls what gets cloned when an academic year rolls over to the next.
 *
 * <p>Every school has exactly one RolloverTemplate (created automatically
 * when the school completes onboarding). Chain branches can inherit from
 * the chain-level template when {@code inheritFromChain = true}.
 *
 * <p>The {@link com.ai.vidya.modules.academic.service.RolloverService}
 * reads this template before each rollover to know which sub-entities
 * to clone and which to leave empty for the admin to fill in fresh.
 *
 * <p>All clone flags default to {@code true} — sensible out-of-the-box
 * behaviour. Admin can toggle off individual flags if they prefer to
 * rebuild that piece from scratch each year (e.g. some schools redesign
 * their timetable every year and don't want the old one as a starting point).
 */
@Entity
@Table(
    name = "rollover_templates",
    indexes = {
        @Index(name = "idx_rt_school_id",      columnList = "school_id", unique = true),
        @Index(name = "idx_rt_auto_rollover",  columnList = "auto_rollover")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolloverTemplate extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    // ── Automation ─────────────────────────────────────────────────────────

    /**
     * When true, the nightly scheduler will automatically create the next
     * academic year when the current year is within {@code rolloverLeadDays}
     * of its end date.
     *
     * When false, rollover must be triggered manually by an admin.
     */
    @Column(name = "auto_rollover", nullable = false)
    @Builder.Default
    private boolean autoRollover = true;

    /**
     * How many days before the current year ends the scheduler should
     * create the next year. Default: 60 days (2 months lead time).
     *
     * This gives admins time to review and adjust the new year's
     * settings before it goes live.
     */
    @Column(name = "rollover_lead_days", nullable = false)
    @Builder.Default
    private int rolloverLeadDays = 60;

    // ── What to clone ──────────────────────────────────────────────────────

    /**
     * Clone SchoolTerm rows and shift dates by the delta between old
     * and new academic year start dates.
     */
    @Column(name = "clone_terms", nullable = false)
    @Builder.Default
    private boolean cloneTerms = true;

    /**
     * Clone SchoolShift rows (shift timings rarely change year to year).
     */
    @Column(name = "clone_shifts", nullable = false)
    @Builder.Default
    private boolean cloneShifts = true;

    /**
     * Clone FeeStructure and all FeeStructureHead children.
     * Amounts are copied verbatim — admin can apply % hike before activation.
     */
    @Column(name = "clone_fee_structure", nullable = false)
    @Builder.Default
    private boolean cloneFeeStructure = true;

    /**
     * Clone SCHOOL_EVENT, PTM_DAY, and MAINTENANCE_BREAK calendar entries.
     * PUBLIC_HOLIDAY entries are never cloned (re-seeded from official sources).
     */
    @Column(name = "clone_holiday_calendar", nullable = false)
    @Builder.Default
    private boolean cloneHolidayCalendar = true;

    /**
     * Clone ExamSchedule rows with dates shifted by the year delta.
     * Useful for schools with fixed exam patterns (e.g. mid-term always in Sep).
     */
    @Column(name = "clone_exam_schedule", nullable = false)
    @Builder.Default
    private boolean cloneExamSchedule = true;

    /**
     * Clone GradingScheme and GradingSchemeEntry rows verbatim.
     * The grading system rarely changes year to year.
     */
    @Column(name = "clone_grading_scheme", nullable = false)
    @Builder.Default
    private boolean cloneGradingScheme = true;

    /**
     * Clone SubjectMapping rows (subjects offered per grade range).
     * Teacher assignments in TimetableSlot are NOT carried over even when
     * cloneTimetable = true.
     */
    @Column(name = "clone_subject_mapping", nullable = false)
    @Builder.Default
    private boolean cloneSubjectMapping = true;

    /**
     * Clone Timetable and TimetableSlot structure.
     * Teacher IDs are set to null in the cloned slots.
     */
    @Column(name = "clone_timetable", nullable = false)
    @Builder.Default
    private boolean cloneTimetable = false;

    // ── Chain inheritance ──────────────────────────────────────────────────

    /**
     * When true and the school is a chain branch, this template's clone flags
     * are ignored and the chain-level RolloverTemplate settings are used instead.
     * Allows a chain admin to manage rollover policy centrally.
     */
    @Column(name = "inherit_from_chain", nullable = false)
    @Builder.Default
    private boolean inheritFromChain = false;

    // ── Notification ───────────────────────────────────────────────────────

    /**
     * Whether to send an in-app and email notification to school admins
     * when the rollover completes (or fails).
     */
    @Column(name = "notify_admins_on_rollover", nullable = false)
    @Builder.Default
    private boolean notifyAdminsOnRollover = true;
}