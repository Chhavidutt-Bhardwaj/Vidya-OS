package com.ai.vidya.modules.student.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.BloodGroup;
import com.ai.vidya.common.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "students",
    indexes = {
        @Index(name = "idx_student_school_id",    columnList = "school_id"),
        @Index(name = "idx_student_admission_no", columnList = "school_id, admission_no"),
        @Index(name = "idx_student_status",       columnList = "student_status"),
        @Index(name = "idx_student_section",      columnList = "current_section_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_admission",
                          columnNames = {"school_id", "admission_no"})
    }
)
@SQLRestriction("is_deleted = false")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Student extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "admission_no", nullable = false, length = 30)
    private String admissionNo;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", length = 15)
    private BloodGroup bloodGroup;

    @Column(name = "aadhar_no", length = 12)
    private String aadharNo;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "religion", length = 50)
    private String religion;

    @Column(name = "caste_category", length = 30)
    private String casteCategory;

    @Column(name = "nationality", length = 50)
    @Builder.Default
    private String nationality = "Indian";

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "mobile_no", length = 15)
    private String mobileNo;

    @Column(name = "current_section_id")
    private UUID currentSectionId;

    @Column(name = "current_roll_no", length = 20)
    private String currentRollNo;

    @Column(name = "current_academic_year_id")
    private UUID currentAcademicYearId;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "admission_class", length = 50)
    private String admissionClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_status", nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "leaving_date")
    private LocalDate leavingDate;

    @Column(name = "leaving_reason", length = 500)
    private String leavingReason;

    // ── Associations ───────────────────────────────────────────────────────
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentGuardian> guardians = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentEnrollment> enrollments = new ArrayList<>();

    // ── Helpers ────────────────────────────────────────────────────────────
    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName);
        sb.append(" ").append(lastName);
        return sb.toString();
    }

    public enum StudentStatus {
        ACTIVE, INACTIVE, TRANSFERRED, LEFT, GRADUATED, DETAINED
    }
}
