package com.ai.vidya.modules.school.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.school.dto.request.*;
import com.ai.vidya.modules.school.dto.response.BulkUploadResult;
import com.ai.vidya.modules.school.dto.response.OnboardingResponse;
import com.ai.vidya.modules.school.service.BulkOnboardingService;
import com.ai.vidya.modules.school.service.SchoolOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for school onboarding.
 *
 * Form-based (step-by-step):
 *   POST /api/v1/onboarding/step/basic-info
 *   POST /api/v1/onboarding/{schoolId}/step/contact
 *   POST /api/v1/onboarding/{schoolId}/step/address
 *   POST /api/v1/onboarding/{schoolId}/step/academic
 *   POST /api/v1/onboarding/{schoolId}/step/document
 *   POST /api/v1/onboarding/{schoolId}/complete
 *   GET  /api/v1/onboarding/{schoolId}/status
 *
 * Bulk upload:
 *   POST /api/v1/onboarding/bulk         (multipart CSV or Excel)
 *
 * All endpoints require SUPER_ADMIN or CHAIN_ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "School Onboarding", description = "Onboard schools individually or in bulk")
public class SchoolOnboardingController {

    private final SchoolOnboardingService onboardingService;
    private final BulkOnboardingService bulkService;

    // ─────────────────────────────────────────────────────────────────────
    // FORM-BASED — step by step
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/step/basic-info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN')")
    @Operation(
        summary     = "Step 1 — Submit basic school info",
        description = "Creates the school record and provisions an admin account. " +
                      "Returns a one-time temporary password — store it securely."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "School created, admin provisioned",
            content      = @Content(schema = @Schema(implementation = OnboardingResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Validation error or UDISE duplicate"
        )
    })
    public ResponseEntity<ApiResponse<OnboardingResponse>> submitBasicInfo(
            @Valid @RequestBody BasicInfoRequest request) {
        OnboardingResponse response = onboardingService.submitBasicInfo(request);
        return ResponseEntity.ok(ApiResponse.success(response,
            "Step 1 complete — admin credentials provisioned"));
    }

    @PostMapping("/{schoolId}/step/contact")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN')")
    @Operation(summary = "Step 2 — Submit contact persons")
    public ResponseEntity<ApiResponse<OnboardingResponse>> submitContacts(
            @Parameter(description = "School UUID from Step 1")
            @PathVariable UUID schoolId,
            @Valid @RequestBody ContactRequest request) {
        OnboardingResponse response = onboardingService.submitContacts(schoolId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Step 2 complete"));
    }

    @PostMapping("/{schoolId}/step/address")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN')")
    @Operation(summary = "Step 3 — Submit physical address")
    public ResponseEntity<ApiResponse<OnboardingResponse>> submitAddress(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AddressRequest request) {
        OnboardingResponse response = onboardingService.submitAddress(schoolId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Step 3 complete"));
    }

    @PostMapping("/{schoolId}/step/academic")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN')")
    @Operation(summary = "Step 4 — Submit academic year, terms, shifts, and grade ranges")
    public ResponseEntity<ApiResponse<OnboardingResponse>> submitAcademicSetup(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AcademicSetupRequest request) {
        OnboardingResponse response = onboardingService.submitAcademicSetup(schoolId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Step 4 complete"));
    }

    @PostMapping("/{schoolId}/step/document")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN')")
    @Operation(
        summary     = "Step 5 — Submit a document",
        description = "Call once per document. Upload the file to S3 first, then submit " +
                      "the returned URL here. Multiple documents can be added."
    )
    public ResponseEntity<ApiResponse<OnboardingResponse>> submitDocument(
            @PathVariable UUID schoolId,
            @Valid @RequestBody DocumentUploadRequest request) {
        OnboardingResponse response = onboardingService.submitDocument(schoolId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Document uploaded"));
    }

    @PostMapping("/{schoolId}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN')")
    @Operation(
        summary     = "Complete onboarding",
        description = "Marks the school as fully onboarded and sets it active on the platform."
    )
    public ResponseEntity<ApiResponse<OnboardingResponse>> completeOnboarding(
            @PathVariable UUID schoolId) {
        OnboardingResponse response = onboardingService.completeOnboarding(schoolId);
        return ResponseEntity.ok(ApiResponse.success(response,
            "Onboarding complete — school is now live"));
    }

    @GetMapping("/{schoolId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHAIN_ADMIN', 'SCHOOL_ADMIN')")
    @Operation(summary = "Get current onboarding status for a school")
    public ResponseEntity<ApiResponse<OnboardingResponse>> getStatus(
            @PathVariable UUID schoolId) {
        OnboardingResponse response = onboardingService.getStatus(schoolId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────────────
    // BULK UPLOAD
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary     = "Bulk onboard schools via CSV or Excel",
        description = """
            Upload a .csv or .xlsx file with one school per row.
            Required columns: school_name, type, phone_primary,
                              address_line1, city, state, pincode.

            Optional but recommended: udise_code, board, medium, official_email,
                                       principal_name, academic_year, admin_email.

            Each row is processed independently — failures do not block other rows.
            The response contains per-row results including temporary admin passwords.

            ⚠ Download the template before uploading:
               GET /api/v1/onboarding/bulk/template
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Processing complete — check results for per-row status",
            content      = @Content(schema = @Schema(implementation = BulkUploadResult.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid file type or missing required columns"
        )
    })
    public ResponseEntity<ApiResponse<BulkUploadResult>> bulkUpload(
            @Parameter(
                description = "CSV or Excel file. Max 500 rows per upload.",
                required    = true
            )
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Uploaded file is empty."));
        }

        BulkUploadResult result = bulkService.process(file);
        String message = String.format(
            "Bulk upload processed: %d success, %d failed out of %d rows.",
            result.getSuccessCount(), result.getFailureCount(), result.getTotalRows()
        );
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @GetMapping(value = "/bulk/template", produces = "text/csv")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary     = "Download CSV template for bulk upload",
        description = "Returns a CSV file with the required header row and one example row."
    )
    public ResponseEntity<byte[]> downloadTemplate() {
        String csv = """
            school_name,type,board,medium,udise_code,affiliation_no,principal_name,official_email,phone_primary,address_line1,address_line2,city,district,state,pincode,academic_year,chain_id,branch_code,branch_name,is_headquarter,admin_email,admin_full_name,admin_phone,established_year,trust_name,management_type,co_ed,residential
            Springfield High School,K12,CBSE,english,12345678901,CBSE-1234,Dr. John Smith,info@springfield.edu,+91-9876543210,12 MG Road,,Springfield,Springfield District,Maharashtra,411001,2024-25,,,,,,,1995,Springfield Education Trust,PRIVATE,true,false
            """;
        return ResponseEntity.ok()
            .header("Content-Disposition",
                "attachment; filename=\"school_bulk_upload_template.csv\"")
            .body(csv.getBytes());
    }
}
