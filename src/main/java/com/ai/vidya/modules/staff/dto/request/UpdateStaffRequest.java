package com.ai.vidya.modules.staff.dto.request;

import com.ai.vidya.modules.staff.entity.Staff;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// ═══════════════════════════════════════════════════════════════════════════
// UpdateStaffRequest  (PUT /api/v1/staff/{id})
// ═══════════════════════════════════════════════════════════════════════════
@Data
public class UpdateStaffRequest {

    @Size(max = 150)
    private String name;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$")
    private String phone;

    private Staff.Department department;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal salary;

    private Staff.StaffStatus status;

    // Teacher
    private String subject;
    private Integer experienceYears;
    private String qualification;

    // Principal
    private BigDecimal leadershipScore;
    private BigDecimal schoolPerformanceRating;

    // Accountant
    private String financeAccessLevel;
    private BigDecimal managedBudget;

    // ExamCoordinator
    private BigDecimal schedulingEfficiency;
    private BigDecimal examPlanningScore;
}
