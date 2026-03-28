package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Maps a teacher to a subject in a specific section.
 *
 * "Teacher Priya teaches Mathematics in Class 10-A during 2024-25"
 *
 * This is the junction that drives:
 *   - Timetable generation (who teaches what where)
 *   - Attendance marking permissions (teacher can only mark for their sections)
 *   - Mark entry permissions (teacher enters marks for their assigned subjects)
 *
 * Many-to-one with SchoolSection.
 * Many-to-one with ClassSubject (subject + class already paired).
 * Teacher UUID → resolves to SystemUser in the teacher module.
 */
@Entity
@Table(
    name = "section_subject_teachers",
    indexes = {
        @Index(name = "idx_sst_section_id",    columnList = "section_id"),
        @Index(name = "idx_sst_teacher_id",    columnList = "teacher_id"),
        @Index(name = "idx_sst_class_subj_id", columnList = "class_subject_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_section_subject",
            columnNames = {"section_id", "class_subject_id"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionSubjectTeacher extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SchoolSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_id", nullable = false)
    private ClassSubject classSubject;

    /**
     * UUID of the assigned teacher (SystemUser with role TEACHER/PRINCIPAL).
     * Not a JPA FK — avoids circular module dependency.
     * Resolved to full user data by the teacher module.
     */
    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    /**
     * Whether this teacher is primary (theory) or secondary (practical/lab).
     * A section-subject can have one theory teacher and one lab teacher.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 15)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.THEORY;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum AssignmentType {
        THEORY,
        PRACTICAL
    }
}
