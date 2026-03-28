package com.ai.vidya.modules.school.service;

import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.response.PageResponse;
import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.school.entity.School;
import com.ai.vidya.modules.school.entity.SchoolChain;
import com.ai.vidya.modules.school.entity.SchoolGradeRange;
import com.ai.vidya.modules.school.repository.SchoolChainRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * All read operations for school / chain / branch profiles.
 * Also handles deactivation and plan changes.
 *<p>
 * All methods return Map<String, Object> so the controller can serve them
 * directly without additional DTO classes. Fields are documented inline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolQueryService {

    private final SchoolRepository      schoolRepository;
    private final SchoolChainRepository chainRepository;
    private final SystemUserRepository  userRepository;

    private static final DateTimeFormatter DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ── GET /api/v1/schools/me ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getSchoolProfile(UUID schoolId) {
        School school = schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        Map<String, Object> result = new LinkedHashMap<>();

        // Core
        result.put("schoolId",   school.getId());
        result.put("name",       school.getName());
        result.put("type",       school.getType());
        result.put("board",      school.getBoard());
        result.put("medium",     school.getMedium());
        result.put("udiseCode",  school.getUdiseCode());
        result.put("plan",       school.getPlan());
        result.put("active",     school.isActive());
        result.put("studentCount", school.getStudentCount());
        result.put("onboardingStep", school.getOnboardingStep());
        result.put("onboardingComplete", school.isOnboardingComplete());

        // Chain info (null for standalone schools)
        if (school.getChain() != null) {
            result.put("chain", Map.of(
                "chainId",   school.getChain().getId(),
                "chainName", school.getChain().getName(),
                "chainCode", school.getChain().getChainCode()
            ));
            result.put("branchCode",  school.getBranchCode());
            result.put("branchName",  school.getBranchName());
            result.put("headquarter", school.isHeadquarter());
        }

        // Basic info (lazy — load if available)
        if (school.getBasicInfo() != null) {
            var bi = school.getBasicInfo();
            result.put("basicInfo", Map.ofEntries(
                Map.entry("tagline",              nullSafe(bi.getTagline())),
                Map.entry("principalName",        nullSafe(bi.getPrincipalName())),
                Map.entry("principalDesignation", nullSafe(bi.getPrincipalDesignation())),
                Map.entry("officialEmail",        nullSafe(bi.getOfficialEmail())),
                Map.entry("website",              nullSafe(bi.getWebsite())),
                Map.entry("phonePrimary",         nullSafe(bi.getPhonePrimary())),
                Map.entry("trustName",            nullSafe(bi.getTrustName())),
                Map.entry("managementType",       nullSafe(bi.getManagementType())),
                Map.entry("establishedYear",      nullSafe(bi.getEstablishedYear())),
                Map.entry("coEd",                 bi.isCoEd()),
                Map.entry("residential",          bi.isResidential()),
                Map.entry("logoUrl",              nullSafe(bi.getLogoUrl()))
            ));
        }

        // Address
        if (school.getAddress() != null) {
            var a = school.getAddress();
            result.put("address", Map.ofEntries(
                Map.entry("addressLine1", nullSafe(a.getAddressLine1())),
                Map.entry("addressLine2", nullSafe(a.getAddressLine2())),
                Map.entry("landmark",     nullSafe(a.getLandmark())),
                Map.entry("city",         nullSafe(a.getCity())),
                Map.entry("district",     nullSafe(a.getDistrict())),
                Map.entry("state",        nullSafe(a.getState())),
                Map.entry("pincode",      nullSafe(a.getPincode())),
                Map.entry("latitude",     nullSafe(a.getLatitude())),
                Map.entry("longitude",    nullSafe(a.getLongitude())),
                Map.entry("mapLink",      nullSafe(a.getMapLink())),
                Map.entry("googlePlaceId",nullSafe(a.getGooglePlaceId()))
            ));
        }

        // Primary contact
        school.getContacts().stream()
            .filter(c -> c.isPrimary() && !c.getDeleted())
            .findFirst()
            .ifPresent(c -> result.put("primaryContact", Map.of(
                "fullName",    c.getFullName(),
                "designation", nullSafe(c.getDesignation()),
                "email",       nullSafe(c.getEmail()),
                "phone",       c.getPhone(),
                "contactType", c.getContactType()
            )));

        // Current academic year
        school.getAcademicYears().stream()
            .filter(ay -> ay.isCurrent() && !ay.getDeleted())
            .findFirst()
            .ifPresent(ay -> result.put("currentAcademicYear", Map.of(
                "academicYearId", ay.getId(),
                "label",          ay.getLabel(),
                "startDate",      ay.getStartDate().toString(),
                "endDate",        ay.getEndDate().toString(),
                "locked",         ay.isLocked(),
                "termCount",      ay.getTerms().size(),
                "shiftCount",     ay.getShifts().size()
            )));

        // Grade ranges summary
        List<Map<String, Object>> grades = new ArrayList<>();
        school.getGradeRanges().stream()
            .filter(g -> !g.getDeleted())
            .sorted(Comparator.comparingInt(SchoolGradeRange::getFromGradeOrder))
            .forEach(g -> grades.add(Map.of(
                "segmentName",    g.getSegmentName(),
                "fromGrade",      g.getFromGrade(),
                "toGrade",        g.getToGrade()
            )));
        result.put("gradeRanges", grades);

        result.put("createdAt", school.getCreatedAt() != null
            ? school.getCreatedAt().format(DT) : null);

        return result;
    }

    // ── GET /api/v1/schools ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listSchools(
            String state, String type, String plan, int page, int size) {

        Page<School> schoolPage = schoolRepository.findAllByFilters(
            state, type, plan,
            PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        List<Map<String, Object>> items = schoolPage.getContent().stream()
            .map(this::schoolSummary)
            .toList();

        return PageResponse.<Map<String, Object>>builder()
            .content(items)
            .page(schoolPage.getNumber())
            .size(schoolPage.getSize())
            .totalElements(schoolPage.getTotalElements())
            .totalPages(schoolPage.getTotalPages())
            .first(schoolPage.isFirst())
            .last(schoolPage.isLast())
            .build();
    }

    // ── GET /api/v1/chains/{chainId} ──────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getChainProfile(UUID chainId) {
        SchoolChain chain = chainRepository.findByIdAndDeletedFalse(chainId)
            .orElseThrow(() -> new ResourceNotFoundException("Chain not found: " + chainId));

        List<School> branches = schoolRepository.findAllByChainIdAndDeletedFalse(chainId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chainId",      chain.getId());
        result.put("name",         chain.getName());
        result.put("chainCode",    chain.getChainCode());
        result.put("description",  chain.getDescription());
        result.put("website",      chain.getWebsite());
        result.put("active",       chain.isActive());
        result.put("branchCount",  branches.size());
        result.put("activeBranchCount",
            branches.stream().filter(School::isActive).count());
        result.put("createdAt", chain.getCreatedAt() != null
            ? chain.getCreatedAt().format(DT) : null);

        // HQ branch summary if present
        branches.stream().filter(School::isHeadquarter).findFirst()
            .ifPresent(hq -> result.put("headquarter", Map.of(
                "schoolId",   hq.getId(),
                "name",       hq.getDisplayName(),
                "branchCode", nullSafe(hq.getBranchCode())
            )));

        // Branch list summary
        List<Map<String, Object>> branchSummaries = branches.stream()
            .map(this::schoolSummary).toList();
        result.put("branches", branchSummaries);

        return result;
    }

    // ── GET /api/v1/chains/{chainId}/branches ─────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listBranches(UUID chainId, int page, int size) {
        // Validate chain exists
        chainRepository.findByIdAndDeletedFalse(chainId)
            .orElseThrow(() -> new ResourceNotFoundException("Chain not found: " + chainId));

        Page<School> branchPage = schoolRepository.findAllByChainIdPaged(
            chainId, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        List<Map<String, Object>> items = branchPage.getContent().stream()
            .map(this::schoolSummary).toList();

        return PageResponse.<Map<String, Object>>builder()
            .content(items)
            .page(branchPage.getNumber())
            .size(branchPage.getSize())
            .totalElements(branchPage.getTotalElements())
            .totalPages(branchPage.getTotalPages())
            .first(branchPage.isFirst())
            .last(branchPage.isLast())
            .build();
    }

    // ── PATCH /api/v1/schools/{schoolId}/deactivate ───────────────────────

    @Transactional
    public void deactivateSchool(UUID schoolId) {
        School school = schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        if (!school.isActive()) {
            throw new BadRequestException("School is already inactive.");
        }
        school.setActive(false);
        schoolRepository.save(school);
        // Deactivate all users of this school
        userRepository.deactivateAllBySchoolId(schoolId);
        log.info("School [{}] deactivated", schoolId);
    }

    // ── PATCH /api/v1/schools/{schoolId}/plan ─────────────────────────────

    @Transactional
    public void changePlan(UUID schoolId, String planName) {
        PlanType newPlan;
        try {
            newPlan = PlanType.valueOf(planName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                "Invalid plan '" + planName + "'. Valid values: " +
                Arrays.toString(PlanType.values()));
        }
        School school = schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        school.setPlan(newPlan);
        schoolRepository.save(school);
        log.info("School [{}] plan changed to {}", schoolId, newPlan);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /** Lightweight school summary — used in list endpoints */
    private Map<String, Object> schoolSummary(School s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schoolId",    s.getId());
        m.put("name",        s.getName());
        m.put("displayName", s.getDisplayName());
        m.put("type",        s.getType());
        m.put("board",       s.getBoard());
        m.put("plan",        s.getPlan());
        m.put("active",      s.isActive());
        m.put("studentCount", s.getStudentCount());
        m.put("onboardingComplete", s.isOnboardingComplete());
        m.put("isChainBranch", s.isChainBranch());
        if (s.isChainBranch()) {
            m.put("chainId",    s.getChain().getId());
            m.put("branchCode", s.getBranchCode());
            m.put("headquarter", s.isHeadquarter());
        }
        if (s.getAddress() != null) {
            m.put("city",    s.getAddress().getCity());
            m.put("state",   s.getAddress().getState());
            m.put("pincode", s.getAddress().getPincode());
        }
        m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().format(DT) : null);
        return m;
    }

    private Object nullSafe(Object val) {
        return val != null ? val : "";
    }
}
