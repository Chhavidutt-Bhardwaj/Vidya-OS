package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.SchoolDay;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

/**
 * A single period slot in a weekly timetable.
 *
 * <p>One row = one period on one day for one class.
 * e.g. Monday, Period 2, 09:00–09:45, Mathematics, Teacher X
 *
 * <p>Rollover: subjects and timings are carried over; teacherId is cleared
 * (set to null) so admin must re-assign teachers for the new year.
 *
 * <p>Clash detection: TimetableService validates before saving that:
 * <ul>
 *   <li>No teacher is assigned to two concurrent slots (across timetables).</li>
 *   <li>No classroom is double-booked at the same time.</li>
 *   <li>Period count per week per subject matches SubjectMapping.periodsPerWeek.</li>
 * </ul>
 */
@Entity
@Table(
    name = "timetable_slots",
    indexes = {
        @Index(name = "idx_ts_timetable_id",  columnList = "timetable_id"),
        @Index(name = "idx_ts_day",           columnList = "school_day"),
        @Index(name = "idx_ts_teacher_id",    columnList = "teacher_id"),
        @Index(name = "idx_ts_subject_code",  columnList = "subject_code")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;

    // ── Slot position ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "school_day", nullable = false, length = 15)
    private SchoolDay schoolDay;

    /** Period sequence within the day: 1, 2, 3 … 8 */
    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // ── Subject ────────────────────────────────────────────────────────────

    /**
     * Subject code — references SubjectMapping.subjectCode for the same year+grade.
     * Stored denormalised (not FK) so historical timetables survive subject renames.
     */
    @Column(name = "subject_code", nullable = false, length = 30)
    private String subjectCode;

    /** Denormalised display name for fast rendering without joins */
    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    // ── Teacher ────────────────────────────────────────────────────────────

    /**
     * UUID of the teacher (SystemUser with TEACHER role) assigned to this slot.
     * NULL when not yet assigned (e.g. freshly cloned timetable after rollover).
     *
     * Stored as UUID (not FK) because teachers live in a different module
     * and will have their own entity when the staff module is built.
     */
    @Column(name = "teacher_id")
    private UUID teacherId;

    /** Denormalised teacher name for fast timetable rendering */
    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    // ── Room ───────────────────────────────────────────────────────────────

    /**
     * Classroom or lab identifier e.g. "Room 12", "Chem Lab 1".
     * Optional — used for clash detection and room utilisation reports.
     */
    @Column(name = "room", length = 50)
    private String room;

    // ── Flags ──────────────────────────────────────────────────────────────

    /**
     * Whether this slot is a free/activity period (library, PT, assembly).
     * Free periods have subjectCode="FREE" and teacherId=null.
     */
    @Column(name = "is_free_period", nullable = false)
    @Builder.Default
    private boolean freePeriod = false;

    /**
     * Whether this slot is a break (recess, lunch).
     * Break slots are not subject to teacher assignment or clash checks.
     */
    @Column(name = "is_break", nullable = false)
    @Builder.Default
    private boolean breakSlot = false;
}