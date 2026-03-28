package com.ai.vidya.modules.staff.dto.response;

import com.ai.vidya.modules.staff.entity.Staff;
import com.ai.vidya.modules.staff.entity.StaffRoleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// ═══════════════════════════════════════════════════════════════════════════
// StaffResponse  — returned for GET/POST/PUT staff endpoints
// ═══════════════════════════════════════════════════════════════════════════
@Data
@Builder
public class StaffResponse {
    private UUID id;
    private UUID tenantId;
    private UUID schoolId;
    private String name;
    private String email;
    private String phone;
    private StaffRoleType roleType;
    private Staff.Department department;
    private LocalDate joiningDate;
    private BigDecimal salary;
    private Staff.StaffStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional subtype fields (null when not applicable)
    private String subject;
    private Integer experienceYears;
    private String qualification;
    private BigDecimal performanceScore;
    private BigDecimal leadershipScore;
    private BigDecimal schoolPerformanceRating;
    private String financeAccessLevel;
    private BigDecimal managedBudget;
    private BigDecimal schedulingEfficiency;
    private BigDecimal examPlanningScore;
}
