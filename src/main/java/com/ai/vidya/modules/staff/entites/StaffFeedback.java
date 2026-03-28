package com.ai.vidya.modules.staff.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// ═══════════════════════════════════════════════════════════════════════════
// StaffFeedback
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Student / parent / peer feedback for a staff member.
 *
 * <p>All feedback is scoped to tenant + school (inherited from {@link BaseEntity}).
 * A composite index on {@code (tenant_id, school_id, staff_id)} ensures fast
 * per-staff queries within a tenant.
 */
@Entity
@Table(
    name = "staff_feedback",
    indexes = {
        @Index(name = "idx_feedback_tenant_school_staff",
               columnList = "tenant_id, school_id, staff_id"),
        @Index(name = "idx_feedback_academic_year",
               columnList = "academic_year")
    }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffFeedback extends BaseEntity {

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    /** Who gave the feedback: STUDENT, PARENT, PEER, ADMIN */
    @Column(name = "feedback_source", nullable = false, length = 20)
    private String feedbackSource;

    /** Rating 1–5 */
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "academic_year", nullable = false, length = 10)
    private String academicYear;

    @Column(name = "feedback_date", nullable = false)
    private LocalDate feedbackDate;
}

// ═══════════════════════════════════════════════════════════════════════════
// StaffPerformance
// ═══════════════════════════════════════════════════════════════════════════

