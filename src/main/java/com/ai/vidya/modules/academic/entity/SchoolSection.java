package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.SchoolShift;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A section/division of a class.
 * e.g. "Class 10 - A", "Class 10 - B", "KG1 - Morning"
 *
 * Sections are the atomic unit of:
 *   - Attendance marking (per section per day)
 *   - Timetable generation (per section)
 *   - Student enrollment (student belongs to one section per year)
 *
 * Many-to-one with SchoolClass.
 * Many-to-one with SchoolShift (optional — overrides class-level shift).
 */
@Entity
@Table(
    name = "school_sections",
    indexes = {
        @Index(name = "idx_section_class_id",   columnList = "class_id"),
        @Index(name = "idx_section_teacher_id", columnList = "class_teacher_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_section_class_name",
            columnNames = {"class_id", "name"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    /**
     * Section identifier — typically a single letter: "A", "B", "C"
     * or a descriptor: "Morning", "Afternoon" for shift-based divisions.
     */
    @Column(nullable = false, length = 10)
    private String name;

    /**
     * UUID of the class teacher (SystemUser with role TEACHER).
     * Stored as UUID reference — not a FK to avoid circular module dependency.
     * The teacher module resolves this to a full user.
     */
    @Column(name = "class_teacher_id")
    private UUID classTeacherId;

    /**
     * Optional room/classroom number for this section.
     * e.g. "Room 201", "Lab Block - 2"
     */
    @Column(length = 30)
    private String room;

    /**
     * Maximum number of students this section can accommodate.
     * Enforced at enrollment time.
     */
    @Column(name = "capacity")
    @Builder.Default
    private int capacity = 40;

    /**
     * Current enrolled student count.
     * Maintained by the student enrollment service.
     */
    @Column(name = "student_count", nullable = false)
    @Builder.Default
    private int studentCount = 0;

    /**
     * Optional shift override. If null, inherits from SchoolClass → AcademicYear default.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private SchoolShift shift;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Helper ──────────────────────────────────────────────────────────────

    public boolean isFull() {
        return studentCount >= capacity;
    }

    public int availableSeats() {
        return Math.max(0, capacity - studentCount);
    }

    public String getFullName() {
        return schoolClass.getName() + " - " + name;
    }
}
