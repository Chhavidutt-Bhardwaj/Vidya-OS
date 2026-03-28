package com.ai.vidya.modules.fee.dto.response;

import com.ai.vidya.modules.fee.entity.FeeInstalment;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeStatementResponse {
    private UUID        studentId;
    private UUID        academicYearId;
    private BigDecimal  totalFee;
    private BigDecimal  totalPaid;
    private BigDecimal  totalPending;
    private BigDecimal  totalDiscount;
    private BigDecimal  totalLateFee;
    private List<InstalmentEntry> instalments;

    @Data @Builder
    public static class InstalmentEntry {
        private UUID                      id;
        private String                    headName;
        private int                       instalmentNumber;
        private BigDecimal                baseAmount;
        private BigDecimal                discountAmount;
        private BigDecimal                lateFeeAmount;
        private BigDecimal                netAmount;
        private BigDecimal                amountPaid;
        private BigDecimal                balance;
        private LocalDate                 dueDate;
        private LocalDate                 paidDate;
        private FeeInstalment.InstalmentStatus status;
        private String                    receiptNo;
    }
}
