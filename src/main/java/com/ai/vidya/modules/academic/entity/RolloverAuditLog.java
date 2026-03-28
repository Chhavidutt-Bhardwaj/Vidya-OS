package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.RolloverStatus;
import com.ai.vidya.common.enums.RolloverTrigger;
import com.ai.vidya.modules.school.entity.AcademicYear;
import com.ai.vidya.modules.school.entity.School;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit record written for every rollover attempt — success or failure.
 *
 * <p>Never updated after creation. One row per rollover attempt per school.
 * If a rollover is retried after failure, a new row is created.
 *
 * <p>The {@code summary} column stores a JSON snapshot of exactly which
 * sub-entities were cloned and how many rows were created in each step.
 * This lets support staff diagnose partial failures without querying
 * every child table.
 *
 * <p>Example summary JSON:
 * <pre>
 * {
 *   "terms":          { "cloned": 2,  "skipped": 0, "failed": 0 },
 *   "shifts":         { "cloned": 1,  "skipped": 0, "failed": 0 },
 *   "feeStructures":  { "cloned": 3,  "skipped": 0, "failed": 0 },
 *   "feeHeads":       { "cloned": 24, "skipped": 0, "failed": 0 },
 *   "holidays":       { "cloned": 18, "skipped": 5, "failed": 0 },
 *   "examSchedules":  { "cloned": 6,  "skipped": 0, "failed": 0 },
 *   "gradingSchemes": { "cloned": 1,  "skipped": 0, "failed": 0 },
 *   "subjectMaps":    { "cloned": 12, "skipped": 0, "failed": 0 },
 *   "timetables":     { "cloned": 0,  "skipped": 1, "failed": 0 }
 * }
 * </pre>
 */
@Entity
@Table(
    name = "rollover_audit_logs",
    indexes = {
        @Index(name = "idx_ral_school_id",      columnList = "school_id"),
        @Index(name = "idx_ral_from_year_id",   columnList = "from_year_id"),
        @Index(name = "idx_ral_to_year_id",     columnList = "to_year_id"),
        @Index(name = "idx_ral_status",         columnList = "status"),
        @Index(name = "idx_ral_trigger",        columnList = "trigger_type"),
        @Index(name = "idx_ral_performed_at",   columnList = "performed_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolloverAuditLog extends BaseEntity {

    // ── Context ────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /**
     * The academic year that was rolled over FROM (source year).
     * May be null only if the rollover failed before the source year could be resolved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_year_id")
    private AcademicYear fromYear;

    /**
     * The new academic year that was created (target year).
     * Null when status = FAILED or SKIPPED and no year was persisted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_year_id")
    private AcademicYear toYear;

    // ── Who / what triggered it ────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private RolloverTrigger triggerType;

    /**
     * UUID of the user who initiated this rollover.
     * Null when triggerType = SCHEDULER (no human actor).
     */
    @Column(name = "performed_by")
    private UUID performedBy;

    // ── Outcome ────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RolloverStatus status;

    /** Wall-clock time when the rollover job started */
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    /** Wall-clock time when the rollover job finished (success or failure) */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Duration in milliseconds — derived from performedAt/completedAt */
    @Column(name = "duration_ms")
    private Long durationMs;

    // ── Detail ─────────────────────────────────────────────────────────────

    /**
     * JSON summary of cloned row counts per entity type.
     * Stored as JSONB on PostgreSQL, JSON on MySQL.
     * Never null on SUCCESS; may be partial on PARTIAL.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary", columnDefinition = "jsonb")
    private String summary;

    /**
     * Human-readable reason when status = SKIPPED.
     * e.g. "Future year 2026-27 already exists for this school."
     *       "Current year is not locked. Rollover requires a locked year."
     *       "autoRollover = false on RolloverTemplate."
     */
    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    /**
     * Exception message + truncated stack trace when status = FAILED or PARTIAL.
     * Stored as TEXT — not shown to end users, only to SUPER_ADMIN in support tools.
     */
    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;
}