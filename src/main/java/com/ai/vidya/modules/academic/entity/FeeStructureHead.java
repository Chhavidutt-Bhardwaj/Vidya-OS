package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.FeeFrequency;
import com.ai.vidya.common.enums.FeeHeadType;
import com.ai.vidya.common.enums.LateFeeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * An individual fee line-item within a FeeStructure.
 *
 * <p>Each row represents one charge type (e.g. Tuition, Transport, Library)
 * with its amount, frequency, due-day, and late-fee rules.
 *
 * <p>On rollover: amounts are copied verbatim. Admins use a bulk-edit screen
 * ("Apply 5% hike to all heads") before activating the new year.
 *
 * <p>GST handling: if SchoolSettings.gstApplicable = true, the fee billing
 * service adds GST on top of {@code amount}. The head itself stores the
 * pre-GST amount — tax computation happens at invoice generation time.
 */
@Entity
@Table(
    name = "fee_structure_heads",
    indexes = {
        @Index(name = "idx_fsh_structure_id",  columnList = "fee_structure_id"),
        @Index(name = "idx_fsh_head_type",     columnList = "head_type"),
        @Index(name = "idx_fsh_optional",      columnList = "optional")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureHead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    // ── Classification ─────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "head_type", nullable = false, length = 30)
    private FeeHeadType headType;

    /**
     * Custom display name for this head.
     * Overrides the enum label in invoices when set.
     * e.g. "Tuition Fee (Standard)" vs just "TUITION".
     */
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    // ── Amount & schedule ──────────────────────────────────────────────────

    /**
     * Per-instalment amount in the school's currency (default INR).
     * For ANNUAL frequency this is the total annual amount.
     * For MONTHLY frequency this is the monthly charge.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private FeeFrequency frequency;

    /**
     * Day of the month (1–28) on which this fee is due.
     * For ANNUAL heads, this is the due day within the first month of the year.
     * The fee billing service uses this to stamp due dates on instalments.
     */
    @Column(name = "due_day_of_month")
    private Integer dueDayOfMonth;

    // ── Late fee ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "late_fee_type", nullable = false, length = 30)
    @Builder.Default
    private LateFeeType lateFeeType = LateFeeType.NONE;

    /**
     * Late fee amount or percentage depending on {@code lateFeeType}:
     * - FLAT → absolute rupee amount added after grace period
     * - PERCENTAGE_PER_MONTH → percentage of outstanding amount per month
     * - NONE → ignored
     */
    @Column(name = "late_fee_value", precision = 10, scale = 2)
    private BigDecimal lateFeeValue;

    // ── Flags ──────────────────────────────────────────────────────────────

    /**
     * Optional heads (e.g. Transport, Hostel) can be opted out by parents
     * during admission. Mandatory heads (e.g. Tuition) cannot be waived.
     */
    @Column(name = "optional", nullable = false)
    @Builder.Default
    private boolean optional = false;

    /** Whether GST is applicable on this specific head (overrides school-level flag) */
    @Column(name = "gst_applicable", nullable = false)
    @Builder.Default
    private boolean gstApplicable = false;

    /** GST rate in percent e.g. 18.00 — used only when gstApplicable = true */
    @Column(name = "gst_rate_pct", precision = 5, scale = 2)
    private BigDecimal gstRatePct;

    /** Whether this head is currently active in the structure */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Display order in fee invoices and ledger screens */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}