package com.ai.vidya.modules.staff.dto.request;

import com.ai.vidya.modules.staff.entity.Staff;
import com.ai.vidya.modules.staff.entity.StaffRoleType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// ═══════════════════════════════════════════════════════════════════════════
// CreateStaffRequest
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Payload for POST /api/v1/staff.
 * Common fields live here; role-specific fields in sub-requests.
 */
@Data
@Builder
public class CreateStaffRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email(message = "Valid email required")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid phone number")
    private String phone;

    @NotNull(message = "Role type is required")
    private StaffRoleType roleType;

    @NotNull(message = "Department is required")
    private Staff.Department department;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date cannot be in the future")
    private LocalDate joiningDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be positive")
    private BigDecimal salary;

    // ── Teacher-specific (required when roleType = TEACHER) ───────────────
    private String subject;
    private Integer experienceYears;
    private String qualification;

    // ── Principal-specific ────────────────────────────────────────────────
    private BigDecimal leadershipScore;

    // ── Accountant-specific ───────────────────────────────────────────────
    private String financeAccessLevel;
    private BigDecimal managedBudget;

    // ── ExamCoordinator-specific ──────────────────────────────────────────
    private BigDecimal schedulingEfficiency;
    private BigDecimal examPlanningScore;
}
