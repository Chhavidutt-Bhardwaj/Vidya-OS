package com.ai.vidya.modules.student.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.GuardianRelation;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "student_guardians",
    indexes = @Index(name = "idx_guardian_student_id", columnList = "student_id")
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentGuardian extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false, length = 20)
    private GuardianRelation relation;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "mobile", nullable = false, length = 15)
    private String mobile;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "aadhar_no", length = 12)
    private String aadharNo;

    @Column(name = "annual_income")
    private Long annualIncome;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(name = "can_pickup", nullable = false)
    @Builder.Default
    private boolean canPickup = true;

    @Column(name = "user_id")
    private UUID userId;
}
