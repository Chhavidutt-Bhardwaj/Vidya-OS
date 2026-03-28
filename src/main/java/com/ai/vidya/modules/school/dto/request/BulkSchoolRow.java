package com.ai.vidya.modules.school.dto.request;

import com.ai.vidya.common.enums.BoardType;
import com.ai.vidya.common.enums.SchoolType;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents one parsed row from a bulk CSV/Excel upload.
 * All fields are strings initially; validation happens in BulkOnboardingService.
 *
 * CSV/Excel column order (header row required):
 *   school_name | type | board | medium | udise_code | affiliation_no |
 *   principal_name | official_email | phone_primary |
 *   address_line1 | city | district | state | pincode |
 *   academic_year | chain_id | branch_code | branch_name | is_headquarter |
 *   admin_email | admin_full_name | admin_phone |
 *   established_year | trust_name | management_type | co_ed | residential
 */
@Data
public class BulkSchoolRow {

    // Row tracking (set by parser — not from sheet)
    private int    rowNumber;
    private String rawLine;

    // ── Core school fields ────────────────────────────────────────────────
    private String schoolName;
    private String type;             // SchoolType enum name
    private String board;            // BoardType enum name
    private String medium;
    private String udiseCode;
    private String affiliationNo;
    private String principalName;
    private String officialEmail;
    private String phonePrimary;

    // ── Address ───────────────────────────────────────────────────────────
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String state;
    private String pincode;

    // ── Academic ──────────────────────────────────────────────────────────
    private String academicYear;     // "2024-25"

    // ── Chain / Branch (optional) ─────────────────────────────────────────
    private String  chainId;         // UUID string — must exist in DB
    private String  branchCode;
    private String  branchName;
    private boolean headquarter;

    // ── Admin account ─────────────────────────────────────────────────────
    private String adminEmail;       // optional; auto-generated if blank
    private String adminFullName;
    private String adminPhone;

    // ── Extended info ─────────────────────────────────────────────────────
    private String  establishedYear;
    private String  trustName;
    private String  managementType;
    private boolean coEd        = true;
    private boolean residential = false;
}


