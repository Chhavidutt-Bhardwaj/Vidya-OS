package com.ai.vidya.modules.student.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "student_enrollments",
    indexes = {
        @Index(name = "idx_enroll_student_id",  columnList = "student_id"),
        @Index(name = "idx_enroll_section_id",  columnList = "section_id"),
        @Index(name = "idx_enroll_year_id",     columnList = "academic_year_id"),
        @Index(name = "idx_enroll_status",      columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_enrollment",
                          columnNames = {"student_id", "academic_year_id"})
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "roll_no", length = 20)
    private String rollNo;

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @Column(name = "section_name", nullable = false, length = 10)
    private String sectionName;

    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "promoted_on")
    private LocalDate promotedOn;

    @Column(name = "promotion_remarks", length = 500)
    private String promotionRemarks;

    public enum EnrollmentStatus {
        ACTIVE, PROMOTED, DETAINED, TRANSFERRED, LEFT
    }
}
