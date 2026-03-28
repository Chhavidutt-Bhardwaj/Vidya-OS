package com.ai.vidya.modules.staff.entites;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID; /**
 * Periodic performance snapshot for a staff member.
 *
 * <p>Computed by {@link com.ai.vidya.modules.staff.service.PerformanceService}
 * and persisted as a time-series record so trends can be tracked across years.
 */
@Entity
@Table(
    name = "staff_performance",
    indexes = {
        @Index(name = "idx_perf_tenant_school_staff",
               columnList = "tenant_id, school_id, staff_id"),
        @Index(name = "idx_perf_academic_year",
               columnList = "academic_year"),
        @Index(name = "idx_perf_score",
               columnList = "overall_score")
    }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformance extends BaseEntity {

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "academic_year", nullable = false, length = 10)
    private String academicYear;

    // ── KPIs ──────────────────────────────────────────────────────────────

    /** Attendance as a percentage (0–100). */
    @Column(name = "attendance_pct", precision = 5, scale = 2)
    private BigDecimal attendancePct;

    /** Student result pass-rate for the teacher's subject (0–100). */
    @Column(name = "result_pct", precision = 5, scale = 2)
    private BigDecimal resultPct;

    /** Average feedback rating (1–5) across all sources. */
    @Column(name = "avg_feedback_rating", precision = 3, scale = 2)
    private BigDecimal avgFeedbackRating;

    /**
     * Composite score (0–100) = weighted average of attendance, results,
     * and feedback. Formula:
     * <pre>
     *   (attendancePct * 0.30) + (resultPct * 0.40) + (avgFeedbackRating * 20 * 0.30)
     * </pre>
     */
    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    // ── Computed period ───────────────────────────────────────────────────

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
}
