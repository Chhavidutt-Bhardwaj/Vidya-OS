package com.ai.vidya.modules.school.service;

import com.ai.vidya.common.enums.BoardType;
import com.ai.vidya.common.enums.OnboardingStep;
import com.ai.vidya.common.enums.SchoolType;
import com.ai.vidya.modules.school.dto.request.BulkSchoolRow;
import com.ai.vidya.modules.school.dto.response.BulkUploadResult;
import com.ai.vidya.modules.school.dto.response.BulkUploadResult.RowResult;
import com.ai.vidya.modules.school.dto.response.OnboardingResponse.AdminCredentials;
import com.ai.vidya.modules.school.entity.*;
import com.ai.vidya.modules.school.repository.SchoolOnboardingAuditRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Processes bulk school uploads from CSV or Excel.
 *
 * Each row is validated independently.
 * Failures are collected and returned in the result — one bad row
 * does NOT abort the rest of the batch.
 *
 * For each valid row:
 *   1. School is created and persisted
 *   2. Admin account is provisioned (same logic as form-based onboarding)
 *   3. Onboarding is immediately marked COMPLETE (bulk = single-shot)
 *   4. School is set active = true
 *
 * Result is returned with per-row success/failure details including
 * the temporary password for each successfully created admin account.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkOnboardingService {

    private final CsvExcelParser                  parser;
    private final SchoolRepository                schoolRepository;
    private final SchoolOnboardingAuditRepository auditRepository;
    private final SchoolAdminProvisioningService  adminProvisioning;

    private static final Pattern PINCODE_RE      = Pattern.compile("^\\d{6}$");
    private static final Pattern UDISE_RE        = Pattern.compile("^\\d{11}$");
    private static final Pattern EMAIL_RE        = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ACADEMIC_YR_RE  = Pattern.compile("^\\d{4}-\\d{2}$");

    // ── Entry point ───────────────────────────────────────────────────────

    public BulkUploadResult process(MultipartFile file) {
        List<BulkSchoolRow> rows = parser.parse(file);
        log.info("Bulk upload: parsed {} rows from {}", rows.size(),
            file.getOriginalFilename());

        List<RowResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (BulkSchoolRow row : rows) {
            RowResult result = processRow(row);
            results.add(result);
            if (result.isSuccess()) successCount++;
            else                    failureCount++;
        }

        log.info("Bulk upload complete: {} success, {} failure out of {} rows",
            successCount, failureCount, rows.size());

        return BulkUploadResult.builder()
            .totalRows(rows.size())
            .successCount(successCount)
            .failureCount(failureCount)
            .skippedCount(0)
            .results(results)
            .build();
    }

    // ── Per-row processing ────────────────────────────────────────────────

    /**
     * Each row is processed in its own transaction so one failure
     * does not roll back successful rows.
     */
    @Transactional
    protected RowResult processRow(BulkSchoolRow row) {
        // 1 — Validate
        List<String> errors = validate(row);
        if (!errors.isEmpty()) {
            return RowResult.builder()
                .rowNumber(row.getRowNumber())
                .success(false)
                .schoolName(row.getSchoolName())
                .validationErrors(errors)
                .errorMessage("Validation failed — see validationErrors.")
                .build();
        }

        try {
            // 2 — Build and persist School
            School school = buildSchool(row);
            schoolRepository.save(school);

            // 3 — Provision admin
            AdminCredentials creds = adminProvisioning.provisionSchoolAdmin(
                school,
                row.getAdminEmail(),
                row.getAdminFullName(),
                row.getAdminPhone()
            );

            // 4 — Mark onboarding complete immediately (bulk = all-in-one)
            school.advanceOnboarding(OnboardingStep.COMPLETE);
            school.setActive(true);
            schoolRepository.save(school);
            logAudit(school, OnboardingStep.COMPLETE, "COMPLETED");

            log.info("Row {}: school [{}] onboarded, admin [{}]",
                row.getRowNumber(), school.getId(), creds.getEmail());

            return RowResult.builder()
                .rowNumber(row.getRowNumber())
                .success(true)
                .schoolId(school.getId())
                .schoolName(school.getName())
                .adminEmail(creds.getEmail())
                .temporaryPassword(creds.getTemporaryPassword())
                .build();

        } catch (Exception ex) {
            log.error("Row {}: failed to process school '{}': {}",
                row.getRowNumber(), row.getSchoolName(), ex.getMessage(), ex);
            return RowResult.builder()
                .rowNumber(row.getRowNumber())
                .success(false)
                .schoolName(row.getSchoolName())
                .errorMessage("Processing error: " + ex.getMessage())
                .build();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────

    private List<String> validate(BulkSchoolRow row) {
        List<String> errors = new ArrayList<>();

        // Required fields
        if (isBlank(row.getSchoolName()))   errors.add("school_name is required");
        if (isBlank(row.getType()))         errors.add("type is required");
        if (isBlank(row.getPhonePrimary())) errors.add("phone_primary is required");
        if (isBlank(row.getAddressLine1())) errors.add("address_line1 is required");
        if (isBlank(row.getCity()))         errors.add("city is required");
        if (isBlank(row.getState()))        errors.add("state is required");
        if (isBlank(row.getPincode()))      errors.add("pincode is required");

        // Enum validation
        if (!isBlank(row.getType())) {
            try { SchoolType.valueOf(row.getType().toUpperCase()); }
            catch (IllegalArgumentException e) {
                errors.add("type '" + row.getType() + "' is invalid. " +
                    "Valid values: " + Arrays.toString(SchoolType.values()));
            }
        }
        if (!isBlank(row.getBoard())) {
            try { BoardType.valueOf(row.getBoard().toUpperCase()); }
            catch (IllegalArgumentException e) {
                errors.add("board '" + row.getBoard() + "' is invalid. " +
                    "Valid values: " + Arrays.toString(BoardType.values()));
            }
        }

        // Format validations
        if (!isBlank(row.getPincode()) && !PINCODE_RE.matcher(row.getPincode()).matches()) {
            errors.add("pincode must be exactly 6 digits");
        }
        if (!isBlank(row.getUdiseCode()) && !UDISE_RE.matcher(row.getUdiseCode()).matches()) {
            errors.add("udise_code must be exactly 11 digits");
        }
        if (!isBlank(row.getOfficialEmail()) && !EMAIL_RE.matcher(row.getOfficialEmail()).matches()) {
            errors.add("official_email is not a valid email address");
        }
        if (!isBlank(row.getAdminEmail()) && !EMAIL_RE.matcher(row.getAdminEmail()).matches()) {
            errors.add("admin_email is not a valid email address");
        }
        if (!isBlank(row.getAcademicYear()) && !ACADEMIC_YR_RE.matcher(row.getAcademicYear()).matches()) {
            errors.add("academic_year must be in format YYYY-YY (e.g. 2024-25)");
        }
        if (!isBlank(row.getEstablishedYear())) {
            try {
                int yr = Integer.parseInt(row.getEstablishedYear());
                if (yr < 1800 || yr > LocalDate.now().getYear()) {
                    errors.add("established_year must be between 1800 and current year");
                }
            } catch (NumberFormatException e) {
                errors.add("established_year must be a 4-digit integer");
            }
        }

        // DB-level uniqueness check (fast — index lookup)
        if (!isBlank(row.getUdiseCode())
                && schoolRepository.existsByUdiseCodeAndDeletedFalse(row.getUdiseCode())) {
            errors.add("udise_code '" + row.getUdiseCode() + "' already exists in the system");
        }

        // Chain branch code uniqueness
        if (!isBlank(row.getChainId()) && !isBlank(row.getBranchCode())) {
            try {
                UUID chainUuid = UUID.fromString(row.getChainId());
                if (schoolRepository.existsByChainIdAndBranchCodeAndDeletedFalse(
                        chainUuid, row.getBranchCode())) {
                    errors.add("branch_code '" + row.getBranchCode() +
                        "' already used in this chain");
                }
            } catch (IllegalArgumentException e) {
                errors.add("chain_id is not a valid UUID");
            }
        }

        return errors;
    }

    // ── Entity builders ───────────────────────────────────────────────────

    private School buildSchool(BulkSchoolRow row) {
        SchoolChain chain = null;
        if (!isBlank(row.getChainId())) {
            chain = new SchoolChain();
            chain.setId(UUID.fromString(row.getChainId()));
        }

        School school = School.builder()
            .chain(chain)
            .branchCode(row.getBranchCode())
            .branchName(row.getBranchName())
            .headquarter(row.isHeadquarter())
            .name(row.getSchoolName().trim())
            .type(SchoolType.valueOf(row.getType().toUpperCase()))
            .board(!isBlank(row.getBoard()) ? BoardType.valueOf(row.getBoard().toUpperCase()) : null)
            .medium(row.getMedium())
            .udiseCode(row.getUdiseCode())
            .affiliationNo(row.getAffiliationNo())
            .onboardingStep(OnboardingStep.BASIC_INFO)
            .onboardingComplete(false)
            .active(false)
            .build();

        // Basic info
        SchoolBasicInfo info = SchoolBasicInfo.builder()
            .school(school)
            .principalName(row.getPrincipalName())
            .officialEmail(row.getOfficialEmail())
            .phonePrimary(row.getPhonePrimary())
            .trustName(row.getTrustName())
            .managementType(!isBlank(row.getManagementType())
                ? row.getManagementType().toUpperCase() : null)
            .establishedYear(!isBlank(row.getEstablishedYear())
                ? Integer.parseInt(row.getEstablishedYear()) : null)
            .coEd(row.isCoEd())
            .residential(row.isResidential())
            .build();
        school.setBasicInfo(info);

        // Address
        SchoolAddress address = SchoolAddress.builder()
            .school(school)
            .addressLine1(row.getAddressLine1())
            .addressLine2(row.getAddressLine2())
            .city(row.getCity())
            .district(row.getDistrict())
            .state(row.getState())
            .pincode(row.getPincode())
            .build();
        school.setAddress(address);

        // Academic year (optional in bulk — default to current Indian academic year if blank)
        String ayLabel = !isBlank(row.getAcademicYear())
            ? row.getAcademicYear()
            : defaultAcademicYearLabel();

        AcademicYear year = buildDefaultAcademicYear(school, ayLabel);
        school.getAcademicYears().add(year);

        return school;
    }

    /**
     * Creates a default single-term, single-shift academic year.
     * Full customization is available post-onboarding in the dashboard.
     */
    private AcademicYear buildDefaultAcademicYear(School school, String label) {
        // Parse "2024-25" → start: 2024-04-01, end: 2025-03-31
        int startYear = Integer.parseInt(label.substring(0, 4));
        LocalDate start = LocalDate.of(startYear, 4, 1);
        LocalDate end   = LocalDate.of(startYear + 1, 3, 31);

        AcademicYear year = AcademicYear.builder()
            .school(school)
            .label(label)
            .startDate(start)
            .endDate(end)
            .current(true)
            .locked(false)
            .build();

        SchoolTerm term = SchoolTerm.builder()
            .academicYear(year)
            .name("Term 1")
            .sortOrder(1)
            .startDate(start)
            .endDate(end)
            .build();
        year.getTerms().add(term);

        SchoolShift shift = SchoolShift.builder()
            .academicYear(year)
            .name("Morning Shift")
            .startTime(java.time.LocalTime.of(8, 0))
            .endTime(java.time.LocalTime.of(14, 0))
            .defaultShift(true)
            .build();
        year.getShifts().add(shift);

        return year;
    }

    private String defaultAcademicYearLabel() {
        int year = LocalDate.now().getYear();
        // Indian academic year: April start
        if (LocalDate.now().getMonthValue() < 4) year--;
        return year + "-" + String.format("%02d", (year + 1) % 100);
    }

    private void logAudit(School school, OnboardingStep step, String action) {
        SchoolOnboardingAudit audit = SchoolOnboardingAudit.builder()
            .school(school)
            .step(step)
            .action(action)
            .performedBy(UUID.fromString("00000000-0000-0000-0000-000000000000")) // system/bulk
            .remarks("Bulk upload")
            .build();
        auditRepository.save(audit);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
