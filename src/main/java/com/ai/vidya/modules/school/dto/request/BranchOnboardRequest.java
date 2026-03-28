package com.ai.vidya.modules.school.dto.request;

import com.ai.vidya.common.enums.BoardType;
import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.enums.SchoolType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request for POST /api/v1/chains/{chainId}/branches
 *<p>
 * Onboards a new school branch under an existing chain.
 * The chainId comes from the path variable — not repeated in the body.
 *<p>
 * A SCHOOL_ADMIN is provisioned for the branch just like standalone onboarding.
 * The CHAIN_ADMIN who created the branch automatically has read access to it
 * via their chainId scope.
 */
@Data
public class BranchOnboardRequest {

    // ── Branch identity ────────────────────────────────────────────────────

    @NotBlank(message = "School/branch name is required")
    @Size(max = 255)
    private String name;

    /**
     * Must be unique within the chain.
     * e.g. "NORTH-01", "SEC62-CAMPUS", "HQ"
     */
    @NotBlank(message = "Branch code is required")
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9_-]{2,30}$",
             message = "Branch code must be 2–30 uppercase alphanumeric characters")
    private String branchCode;

    /** Human-readable campus label: "Sector 62 Campus", "Main Branch" */
    @Size(max = 100)
    private String branchName;

    /**
     * If true, marks this as the HQ / admin branch.
     * Only one branch per chain can be HQ — service enforces this.
     */
    private boolean headquarter = false;

    @NotNull(message = "School type is required")
    private SchoolType type;

    private BoardType board;

    @Size(max = 30)
    private String medium;

    @Pattern(regexp = "^\\d{11}$", message = "UDISE code must be 11 digits")
    private String udiseCode;

    @Size(max = 50)
    private String affiliationNo;

    private PlanType plan = PlanType.STARTER;

    // ── Principal / contact ────────────────────────────────────────────────

    @Size(max = 150)
    private String principalName;

    @Size(max = 100)
    private String principalDesignation;

    @Email @Size(max = 255)
    private String officialEmail;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String phonePrimary;

    // ── Address (required) ─────────────────────────────────────────────────

    @NotNull(message = "Address is required")
    @Valid
    private SchoolOnboardRequest.AddressData address;

    // ── Academic year (optional) ───────────────────────────────────────────

    @Valid
    private SchoolOnboardRequest.AcademicData academic;

    // ── Admin account for this branch ─────────────────────────────────────

    @Email @Size(max = 255)
    private String adminEmail;

    @Size(max = 150)
    private String adminFullName;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String adminPhone;
}
