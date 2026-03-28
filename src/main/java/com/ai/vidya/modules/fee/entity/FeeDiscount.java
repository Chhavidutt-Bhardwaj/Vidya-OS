package com.ai.vidya.modules.fee.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_discounts",
       indexes = @Index(name = "idx_fd_student_id", columnList = "student_id, academic_year_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeeDiscount extends BaseEntity {

    @Column(name = "school_id",        nullable = false) private UUID schoolId;
    @Column(name = "student_id",       nullable = false) private UUID studentId;
    @Column(name = "academic_year_id", nullable = false) private UUID academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_name", nullable = false, length = 100)
    private String discountName;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_mode", nullable = false, length = 15)
    private DiscountMode discountMode;

    @Column(name = "max_cap", precision = 10, scale = 2)
    private BigDecimal maxCap;

    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_to")                     private LocalDate validTo;
    @Column(name = "approved_by", nullable = false) private UUID approvedBy;
    @Column(name = "remarks", length = 500)         private String remarks;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum DiscountType   { SIBLING, SCHOLARSHIP, MERIT, STAFF_WARD, CUSTOM, EARLY_BIRD, BULK }
    public enum DiscountMode   { PERCENTAGE, FLAT }
}
