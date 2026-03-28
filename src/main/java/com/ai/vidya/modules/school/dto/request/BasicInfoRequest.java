package com.ai.vidya.modules.school.dto.request;

import com.ai.vidya.common.enums.BoardType;
import com.ai.vidya.common.enums.SchoolType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Step 1 — Basic school identity.
 * This is the first form submitted; creates the School row and issues the admin account.
 */
@Data
public class BasicInfoRequest {

    @NotBlank(message = "School name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "School type is required")
    private SchoolType type;

    private BoardType board;

    @Size(max = 30, message = "Medium must be at most 30 characters")
    private String medium;          // english, hindi, tamil …

    @Pattern(regexp = "^\\d{11}$", message = "UDISE code must be 11 digits")
    private String udiseCode;

    @Size(max = 50)
    private String affiliationNo;

    // ── Chain / Branch (optional — only when onboarding a chain branch) ──

    private java.util.UUID chainId;       // null = standalone school
    private String         branchCode;    // e.g. "NORTH-01"
    private String         branchName;    // e.g. "North Campus"
    private boolean        headquarter;   // true if this branch is the HQ

    // ── Extended basic info (SchoolBasicInfo table) ──────────────────────

    @Size(max = 500)
    private String tagline;

    @Size(max = 150)
    private String principalName;

    @Size(max = 100)
    private String principalDesignation;

    @Email(message = "Official email must be valid")
    @Size(max = 255)
    private String officialEmail;

    @Size(max = 255)
    private String website;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String phonePrimary;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String phoneSecondary;

    private Integer   establishedYear;
    private LocalDate foundedOn;

    @Size(max = 255)
    private String trustName;

    @Pattern(regexp = "GOVERNMENT|PRIVATE|AIDED|AUTONOMOUS|OTHER",
             message  = "Invalid management type")
    private String managementType;

    private boolean coEd        = true;
    private boolean residential = false;

    // ── School Admin account details (created at step 1) ─────────────────

    /**
     * Optional override for the admin email.
     * If null, generated as: admin.<slugName>@vidya.ai
     */
    @Email
    @Size(max = 255)
    private String adminEmail;

    /**
     * Optional override for the admin's full name.
     * Defaults to principalName if provided, else "School Admin".
     */
    @Size(max = 150)
    private String adminFullName;

    /**
     * Optional admin phone. Receives credentials via SMS if provided.
     */
    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String adminPhone;
}