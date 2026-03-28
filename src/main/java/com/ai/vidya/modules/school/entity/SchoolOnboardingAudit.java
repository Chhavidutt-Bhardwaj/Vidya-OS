package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.OnboardingStep;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Immutable audit log of every onboarding step transition.
 * Useful for support, debugging, and resuming interrupted onboarding.
 *
 * Many-to-one with School.
 */
@Entity
@Table(
    name = "school_onboarding_audit",
    indexes = {
        @Index(name = "idx_onboard_audit_school_id", columnList = "school_id"),
        @Index(name = "idx_onboard_audit_step",      columnList = "step")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolOnboardingAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false, length = 30)
    private OnboardingStep step;

    /** COMPLETED, SKIPPED, REVERTED */
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    /** UUID of the user (admin) who performed the action */
    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    /** Optional metadata e.g. IP address, device */
    @Column(name = "remarks", length = 500)
    private String remarks;
}