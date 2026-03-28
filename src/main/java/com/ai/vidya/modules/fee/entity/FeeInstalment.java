package com.ai.vidya.modules.fee.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.academic.entity.FeeStructureHead;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "fee_instalments",
    indexes = {
        @Index(name = "idx_fi_student_id",  columnList = "student_id, academic_year_id"),
        @Index(name = "idx_fi_due_date",    columnList = "due_date"),
        @Index(name = "idx_fi_status",      columnList = "status"),
        @Index(name = "idx_fi_school_year", columnList = "school_id, academic_year_id")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeeInstalment extends BaseEntity {

    @Column(name = "school_id",        nullable = false) private UUID schoolId;
    @Column(name = "student_id",       nullable = false) private UUID studentId;
    @Column(name = "academic_year_id", nullable = false) private UUID academicYearId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_head_id", nullable = false)
    private FeeStructureHead feeStructureHead;

    @Column(name = "instalment_number", nullable = false) private int instalmentNumber;

    @Column(name = "base_amount",     nullable = false, precision = 12, scale = 2) private BigDecimal baseAmount;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "late_fee_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default private BigDecimal lateFeeAmount = BigDecimal.ZERO;
    @Column(name = "gst_amount",      nullable = false, precision = 12, scale = 2)
    @Builder.Default private BigDecimal gstAmount = BigDecimal.ZERO;
    @Column(name = "net_amount",      nullable = false, precision = 12, scale = 2) private BigDecimal netAmount;
    @Column(name = "amount_paid",     nullable = false, precision = 12, scale = 2)
    @Builder.Default private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "due_date",  nullable = false) private LocalDate dueDate;
    @Column(name = "paid_date")                   private LocalDate paidDate;
    @Column(name = "receipt_no", length = 30)     private String   receiptNo;
    @Column(name = "waiver_reason", length = 500) private String   waiverReason;
    @Column(name = "waived_by")                   private UUID     waivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InstalmentStatus status = InstalmentStatus.PENDING;

    public BigDecimal getBalance()  { return netAmount.subtract(amountPaid); }
    public boolean    isOverdue()   { return status == InstalmentStatus.PENDING && LocalDate.now().isAfter(dueDate); }

    public enum InstalmentStatus { PENDING, PARTIAL, PAID, OVERDUE, WAIVED, CANCELLED }
}
