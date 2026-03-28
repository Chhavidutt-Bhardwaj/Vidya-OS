package com.ai.vidya.modules.staff.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

// ═══════════════════════════════════════════════════════════════════════════
// Principal
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Principal — school head. Tracks leadership and overall school performance.
 */
@Entity
@Table(name = "principals")
@DiscriminatorValue("PRINCIPAL")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
class Principal extends Staff {

    /** Leadership effectiveness score (0–100). */
    @Column(name = "leadership_score", precision = 5, scale = 2)
    private BigDecimal leadershipScore;

    /** Aggregated school performance rating derived from all teacher scores. */
    @Column(name = "school_performance_rating", precision = 5, scale = 2)
    private BigDecimal schoolPerformanceRating;
}

// ═══════════════════════════════════════════════════════════════════════════
// Accountant
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Accountant — manages school finances.
 */
@Entity
@Table(name = "accountants")
@DiscriminatorValue("ACCOUNTANT")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
class Accountant extends Staff {

    /**
     * Access tier: BASIC, STANDARD, FULL.
     * Controls which finance reports the accountant can view/export.
     */
    @Column(name = "finance_access_level", length = 20)
    private String financeAccessLevel;

    /** Total budget (INR) this accountant is responsible for. */
    @Column(name = "managed_budget", precision = 15, scale = 2)
    private BigDecimal managedBudget;
}

// ═══════════════════════════════════════════════════════════════════════════
// ExamCoordinator
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Exam Coordinator — handles exam scheduling and planning.
 */
@Entity
@Table(name = "exam_coordinators")
@DiscriminatorValue("EXAM_COORDINATOR")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
class ExamCoordinator extends Staff {

    /** % of exam schedules released on time (0–100). */
    @Column(name = "scheduling_efficiency", precision = 5, scale = 2)
    private BigDecimal schedulingEfficiency;

    /** Planning quality score derived from last cycle post-mortem (0–100). */
    @Column(name = "exam_planning_score", precision = 5, scale = 2)
    private BigDecimal examPlanningScore;
}

// ═══════════════════════════════════════════════════════════════════════════
// HR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * HR Staff member — manages recruitment and employee relations.
 */
@Entity
@Table(name = "hr_staff")
@DiscriminatorValue("HR")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
class HrStaff extends Staff {

    /** Number of open positions currently being recruited for. */
    @Column(name = "open_positions")
    private Integer openPositions;

    /** Average days to fill a vacancy. */
    @Column(name = "avg_hiring_days")
    private Integer avgHiringDays;
}
