package com.ai.vidya.modules.fee.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "fee_payments",
    indexes = {
        @Index(name = "idx_fp_student_id", columnList = "student_id, academic_year_id"),
        @Index(name = "idx_fp_receipt_no", columnList = "school_id, receipt_no"),
        @Index(name = "idx_fp_date",       columnList = "payment_date")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeePayment extends BaseEntity {

    @Column(name = "school_id",        nullable = false) private UUID      schoolId;
    @Column(name = "student_id",       nullable = false) private UUID      studentId;
    @Column(name = "academic_year_id", nullable = false) private UUID      academicYearId;
    @Column(name = "receipt_no",       nullable = false, length = 30) private String receiptNo;
    @Column(name = "payment_date",     nullable = false) private LocalDate paymentDate;
    @Column(name = "total_amount",     nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(name = "transaction_ref", length = 100) private String transactionRef;
    @Column(name = "bank_name",       length = 100) private String bankName;
    @Column(name = "cheque_no",       length = 50)  private String chequeNo;
    @Column(name = "collected_by",    nullable = false) private UUID collectedBy;
    @Column(name = "remarks",         length = 500) private String remarks;

    @Column(name = "is_refunded")  @Builder.Default private boolean isRefunded = false;
    @Column(name = "refunded_amount", precision = 12, scale = 2) private BigDecimal refundedAmount;
    @Column(name = "refund_date")  private LocalDate refundDate;
    @Column(name = "refund_reason", length = 500) private String refundReason;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FeePaymentItem> items = new ArrayList<>();
}
