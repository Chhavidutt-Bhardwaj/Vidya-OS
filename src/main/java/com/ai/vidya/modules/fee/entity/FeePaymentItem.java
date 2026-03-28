package com.ai.vidya.modules.fee.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_payment_items",
       indexes = {
           @Index(name = "idx_fpi_payment_id",    columnList = "payment_id"),
           @Index(name = "idx_fpi_instalment_id", columnList = "instalment_id")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeePaymentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private FeePayment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instalment_id", nullable = false)
    private FeeInstalment instalment;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
