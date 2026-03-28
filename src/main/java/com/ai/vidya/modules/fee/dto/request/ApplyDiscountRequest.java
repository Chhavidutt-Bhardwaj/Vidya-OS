package com.ai.vidya.modules.fee.dto.request;

import com.ai.vidya.modules.fee.entity.FeeDiscount;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApplyDiscountRequest {

    @NotNull  private FeeDiscount.DiscountType discountType;
    @NotBlank private String                   discountName;

    @NotNull @DecimalMin("0.01")
    private BigDecimal discountValue;

    @NotNull  private FeeDiscount.DiscountMode  discountMode;
    private   BigDecimal                        maxCap;

    @NotNull  private LocalDate validFrom;
    private   LocalDate         validTo;
    private   String            remarks;
}
