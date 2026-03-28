package com.ai.vidya.modules.fee.dto.request;

import com.ai.vidya.common.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CollectFeeRequest {

    @NotNull private UUID           studentId;
    @NotNull private UUID           academicYearId;
    @NotNull private UUID           collectedBy;

    @NotEmpty(message = "At least one instalment must be selected")
    private List<UUID> instalmentIds;

    @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull private PaymentMode    paymentMode;
    private LocalDate               paymentDate;
    private String                  transactionRef;
    private String                  bankName;
    private String                  chequeNo;
    private String                  remarks;
}
