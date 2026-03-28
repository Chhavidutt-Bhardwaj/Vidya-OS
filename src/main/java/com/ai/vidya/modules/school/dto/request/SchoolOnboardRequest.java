package com.ai.vidya.modules.school.dto.request;

import com.ai.vidya.common.enums.BoardType;
import com.ai.vidya.common.enums.ContactType;
import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.enums.SchoolType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Single-shot request for onboarding a standalone school via
 * POST /api/v1/schools
 *
 * Bundles all 5 onboarding steps into one payload so the caller
 * does not need to drive the step-by-step flow. Used by SUPER_ADMIN
 * when onboarding from the admin dashboard in one pass.
 *
 * All nested objects mirror the individual step DTOs but are grouped
 * here as inner classes to keep the API surface clean.
 */
@Data
public class SchoolOnboardRequest {

    // ── School identity ────────────────────────────────────────────────────

    @NotBlank(message = "School name is required")
    @Size(max = 255)
    private String name;

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

    // ── Extended basic info ────────────────────────────────────────────────

    @Size(max = 500)
    private String tagline;

    @Size(max = 150)
    private String principalName;

    @Size(max = 100)
    private String principalDesignation;

    @Email
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
             message = "Invalid management type")
    private String managementType;

    private boolean coEd        = true;
    private boolean residential = false;

    // ── Address (required) ─────────────────────────────────────────────────

    @NotNull(message = "Address is required")
    @Valid
    private AddressData address;

    @Data
    public static class AddressData {

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255)
        private String addressLine1;

        @Size(max = 255)
        private String addressLine2;

        @Size(max = 255)
        private String landmark;

        @NotBlank(message = "City is required")
        @Size(max = 100)
        private String city;

        @Size(max = 100)
        private String district;

        @NotBlank(message = "State is required")
        @Size(max = 100)
        private String state;

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
        private String pincode;

        @DecimalMin("-90.0") @DecimalMax("90.0")
        private BigDecimal latitude;

        @DecimalMin("-180.0") @DecimalMax("180.0")
        private BigDecimal longitude;

        @Size(max = 512)
        private String mapLink;

        @Size(max = 100)
        private String googlePlaceId;

        private String directions;
    }

    // ── Primary contact (required) ─────────────────────────────────────────

    @NotNull(message = "Primary contact is required")
    @Valid
    private ContactData primaryContact;

    @Data
    public static class ContactData {

        private ContactType contactType = ContactType.PRINCIPAL;

        @NotBlank(message = "Contact full name is required")
        @Size(max = 150)
        private String fullName;

        @Size(max = 100)
        private String designation;

        @Email @Size(max = 255)
        private String email;

        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
        private String phone;

        private boolean receiveNotifications = true;
    }

    // ── Academic year (optional — defaults to current Indian academic year) ─

    @Valid
    private AcademicData academic;

    @Data
    public static class AcademicData {

        @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Label must be YYYY-YY e.g. 2024-25")
        private String label;

        private LocalDate startDate;
        private LocalDate endDate;
    }

    // ── Settings overrides (optional — all default to SchoolSettings defaults) ─

    @Valid
    private SettingsData settings;

    @Data
    public static class SettingsData {
        private String  locale                  = "en-IN";
        private String  timezone                = "Asia/Kolkata";
        private Integer academicYearStartMonth  = 4;
        private Integer minAttendancePct        = 75;
        private boolean saturdayWorking         = true;
        private boolean gstApplicable           = false;
        private String  gstin;
        private boolean smsEnabled              = false;
        private boolean whatsappEnabled         = false;
        private String  brandColorPrimary;
        private String  brandColorSecondary;
    }

    // ── Admin account ─────────────────────────────────────────────────────

    @Email @Size(max = 255)
    private String adminEmail;      // null = auto-generate from school name

    @Size(max = 150)
    private String adminFullName;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,20}$", message = "Invalid phone number")
    private String adminPhone;
}
