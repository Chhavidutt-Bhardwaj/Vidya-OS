package com.ai.vidya.modules.school.service;

import com.ai.vidya.common.enums.ContactType;
import com.ai.vidya.common.enums.OnboardingStep;
import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.enums.UserType;
import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.school.dto.request.*;
import com.ai.vidya.modules.school.dto.response.*;
import com.ai.vidya.modules.school.entity.*;
import com.ai.vidya.modules.school.repository.SchoolChainRepository;
import com.ai.vidya.modules.school.repository.SchoolOnboardingAuditRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import com.ai.vidya.modules.user.entity.Role;
import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.RoleRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import com.ai.vidya.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * Handles all school/chain/branch creation paths.
 *<p>
 * Three entry points:
 *<p>
 *   onboardStandaloneSchool(req)         → POST /api/v1/schools
 *       Creates a standalone school (no chain) with address, contact,
 *       default academic year, default settings, and a SCHOOL_ADMIN user.
 *       All data arrives in one payload — no step machine.
 *<p>
 *   createChain(req)                     → POST /api/v1/chains
 *       Creates a SchoolChain record and provisions a CHAIN_ADMIN user.
 *       Does not create any branch schools — branches are added separately.
 *<p>
 *   onboardBranch(chainId, req)          → POST /api/v1/chains/{chainId}/branches
 *       Adds a School branch under an existing chain.
 *       Validates: chain exists, branch code unique within chain,
 *       at most one HQ branch per chain.
 *       Provisions a SCHOOL_ADMIN for the branch.
 *<p>
 * The step-by-step methods (submitBasicInfo, submitContacts, etc.)
 * remain unchanged for the multi-step onboarding flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolOnboardingService {

    private static final String SCHOOL_ADMIN_ROLE = "SCHOOL_ADMIN";
    private static final String CHAIN_ADMIN_ROLE  = "CHAIN_ADMIN";
    private static final String LOGIN_URL         = "https://app.vidya.ai/login";

    private final SchoolRepository                schoolRepository;
    private final SchoolChainRepository           chainRepository;
    private final SchoolOnboardingAuditRepository auditRepository;
    private final SchoolAdminProvisioningService  adminProvisioning;
    private final RoleRepository                  roleRepository;
    private final SystemUserRepository            userRepository;
    private final PasswordEncoder                 passwordEncoder;
    private final UsernamePasswordGeneratorService generator;

    // ══════════════════════════════════════════════════════════════════════
    // 1. STANDALONE SCHOOL ONBOARDING  — POST /api/v1/schools
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a complete standalone school in a single atomic transaction.
     * Provisions a SCHOOL_ADMIN user and returns their one-time credentials.
     */
    @Transactional
    public SchoolOnboardResponse onboardStandaloneSchool(SchoolOnboardRequest req) {

        // ── Uniqueness guards ────────────────────────────────────────────
        if (req.getUdiseCode() != null
                && schoolRepository.existsByUdiseCodeAndDeletedFalse(req.getUdiseCode())) {
            throw new BadRequestException(
                "A school with UDISE code " + req.getUdiseCode() + " already exists.");
        }

        // ── Build School ─────────────────────────────────────────────────
        School school = School.builder()
            .name(req.getName())
            .type(req.getType())
            .board(req.getBoard())
            .medium(req.getMedium())
            .udiseCode(req.getUdiseCode())
            .affiliationNo(req.getAffiliationNo())
            .plan(req.getPlan() != null ? req.getPlan() : PlanType.STARTER)
            .onboardingStep(OnboardingStep.BASIC_INFO)
            .onboardingComplete(false)
            .active(false)   // activated at end of method
            .build();

        // ── BasicInfo ────────────────────────────────────────────────────
        SchoolBasicInfo info = SchoolBasicInfo.builder()
            .school(school)
            .tagline(req.getTagline())
            .principalName(req.getPrincipalName())
            .principalDesignation(req.getPrincipalDesignation())
            .officialEmail(req.getOfficialEmail())
            .website(req.getWebsite())
            .phonePrimary(req.getPhonePrimary())
            .phoneSecondary(req.getPhoneSecondary())
            .establishedYear(req.getEstablishedYear())
            .foundedOn(req.getFoundedOn())
            .trustName(req.getTrustName())
            .managementType(req.getManagementType())
            .coEd(req.isCoEd())
            .residential(req.isResidential())
            .build();
        school.setBasicInfo(info);

        // ── Address ──────────────────────────────────────────────────────
        school.setAddress(buildAddress(school, req.getAddress()));

        // ── Primary contact ──────────────────────────────────────────────
        school.getContacts().add(buildContact(school, req.getPrimaryContact()));

        // ── Default settings ─────────────────────────────────────────────
        school.setSettings(buildSettings(school, req.getSettings()));

        // ── Academic year ────────────────────────────────────────────────
        school.getAcademicYears().add(buildDefaultAcademicYear(school, req.getAcademic()));

        // ── Persist school and all cascaded children ─────────────────────
        schoolRepository.save(school);

        // ── Provision SCHOOL_ADMIN ───────────────────────────────────────
        String rawPassword    = generator.generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        String adminEmail     = resolveAdminEmail(req.getAdminEmail(), school.getName());
        String adminFullName  = resolveAdminName(req.getAdminFullName(), req.getPrincipalName());

        Role adminRole = requireRole(SCHOOL_ADMIN_ROLE);
        SystemUser adminUser = SystemUser.builder()
            .email(adminEmail)
            .passwordHash(hashedPassword)
            .fullName(adminFullName)
            .phone(req.getAdminPhone())
            .userType(UserType.SCHOOL_ADMIN)
            .schoolId(school.getId())
            .active(true)
            .mustChangePassword(true)
            .roles(Set.of(adminRole))
            .build();
        userRepository.save(adminUser);

        // ── Mark onboarding complete, activate school ────────────────────
        school.advanceOnboarding(OnboardingStep.COMPLETE);
        school.setActive(true);
        schoolRepository.save(school);
        logAudit(school, OnboardingStep.COMPLETE, "COMPLETED");

        log.info("Standalone school [{}] onboarded, admin [{}]", school.getId(), adminEmail);

        return SchoolOnboardResponse.builder()
            .schoolId(school.getId())
            .schoolName(school.getName())
            .schoolType(school.getType())
            .plan(school.getPlan())
            .udiseCode(school.getUdiseCode())
            .city(req.getAddress().getCity())
            .state(req.getAddress().getState())
            .pincode(req.getAddress().getPincode())
            .active(true)
            .onboardingComplete(true)
            .createdAt(LocalDateTime.now())
            .adminCredentials(SchoolOnboardResponse.AdminCredentials.builder()
                .userId(adminUser.getId())
                .email(adminEmail)
                .username(adminEmail)
                .temporaryPassword(rawPassword)
                .loginUrl(LOGIN_URL)
                .note("Temporary password — must be changed on first login.")
                .build())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. CREATE CHAIN  — POST /api/v1/chains
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new school chain and provisions a CHAIN_ADMIN user.
     * Does NOT create any branch schools — use onboardBranch() for that.
     */
    @Transactional
    public ChainCreateResponse createChain(ChainCreateRequest req) {

        // ── Uniqueness guard ─────────────────────────────────────────────
        if (chainRepository.existsByChainCodeAndDeletedFalse(req.getChainCode())) {
            throw new BadRequestException(
                "A chain with code '" + req.getChainCode() + "' already exists.");
        }

        // ── Build and persist chain ──────────────────────────────────────
        SchoolChain chain = SchoolChain.builder()
            .name(req.getName())
            .chainCode(req.getChainCode().toUpperCase())
            .description(req.getDescription())
            .website(req.getWebsite())
            .active(true)
            .build();
        chainRepository.save(chain);

        // ── Provision CHAIN_ADMIN ────────────────────────────────────────
        String rawPassword    = generator.generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        String adminEmail     = (req.getAdminEmail() != null && !req.getAdminEmail().isBlank())
            ? req.getAdminEmail().trim().toLowerCase()
            : "admin." + chain.getChainCode().toLowerCase() + "@vidya.ai";

        if (userRepository.existsByEmailAndDeletedFalse(adminEmail)) {
            throw new BadRequestException(
                "An account with email '" + adminEmail + "' already exists.");
        }

        Role chainAdminRole = requireRole(CHAIN_ADMIN_ROLE);
        SystemUser chainAdmin = SystemUser.builder()
            .email(adminEmail)
            .passwordHash(hashedPassword)
            .fullName(req.getAdminFullName())
            .phone(req.getAdminPhone())
            .userType(UserType.CHAIN_ADMIN)
            .chainId(chain.getId())
            .active(true)
            .mustChangePassword(true)
            .roles(Set.of(chainAdminRole))
            .build();
        userRepository.save(chainAdmin);

        log.info("Chain [{}] created, chain admin [{}]", chain.getId(), adminEmail);

        return ChainCreateResponse.builder()
            .chainId(chain.getId())
            .chainName(chain.getName())
            .chainCode(chain.getChainCode())
            .website(chain.getWebsite())
            .active(true)
            .createdAt(LocalDateTime.now())
            .chainAdminCredentials(ChainCreateResponse.ChainAdminCredentials.builder()
                .userId(chainAdmin.getId())
                .email(adminEmail)
                .username(adminEmail)
                .temporaryPassword(rawPassword)
                .loginUrl(LOGIN_URL)
                .note("Temporary password — must be changed on first login.")
                .build())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. ONBOARD BRANCH  — POST /api/v1/chains/{chainId}/branches
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new school branch under an existing chain.
     *<p>
     * Enforces:
     *   - Chain must exist and be active
     *   - branchCode must be unique within the chain
     *   - At most one HQ branch per chain (if req.isHeadquarter() == true)
     *   - UDISE code globally unique (if provided)
     *<p>
     * Provisions a SCHOOL_ADMIN user scoped to both schoolId and chainId.
     */
    @Transactional
    public BranchOnboardResponse onboardBranch(UUID chainId, BranchOnboardRequest req) {

        // ── Resolve and validate chain ───────────────────────────────────
        SchoolChain chain = chainRepository.findByIdAndDeletedFalse(chainId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Chain not found: " + chainId));

        if (!chain.isActive()) {
            throw new BadRequestException(
                "Chain '" + chain.getName() + "' is inactive. Cannot add branches.");
        }

        // ── Branch code uniqueness within chain ──────────────────────────
        if (schoolRepository.existsByChainIdAndBranchCodeAndDeletedFalse(
                chainId, req.getBranchCode())) {
            throw new BadRequestException(
                "Branch code '" + req.getBranchCode() +
                "' is already used in chain '" + chain.getName() + "'.");
        }

        // ── HQ uniqueness: only one HQ per chain ─────────────────────────
        if (req.isHeadquarter()) {
            schoolRepository.findHeadquarterByChainId(chainId).ifPresent(existing -> {
                throw new BadRequestException(
                    "Chain '" + chain.getName() +
                    "' already has a headquarter branch: '" + existing.getDisplayName() + "'. " +
                    "Demote the existing HQ before promoting a new one.");
            });
        }

        // ── UDISE uniqueness ─────────────────────────────────────────────
        if (req.getUdiseCode() != null
                && schoolRepository.existsByUdiseCodeAndDeletedFalse(req.getUdiseCode())) {
            throw new BadRequestException(
                "A school with UDISE code " + req.getUdiseCode() + " already exists.");
        }

        // ── Build branch School ──────────────────────────────────────────
        School branch = School.builder()
            .chain(chain)
            .branchCode(req.getBranchCode().toUpperCase())
            .branchName(req.getBranchName())
            .headquarter(req.isHeadquarter())
            .name(req.getName())
            .type(req.getType())
            .board(req.getBoard())
            .medium(req.getMedium())
            .udiseCode(req.getUdiseCode())
            .affiliationNo(req.getAffiliationNo())
            .plan(req.getPlan() != null ? req.getPlan() : PlanType.STARTER)
            .onboardingStep(OnboardingStep.BASIC_INFO)
            .onboardingComplete(false)
            .active(false)
            .build();

        // ── BasicInfo ────────────────────────────────────────────────────
        SchoolBasicInfo info = SchoolBasicInfo.builder()
            .school(branch)
            .principalName(req.getPrincipalName())
            .principalDesignation(req.getPrincipalDesignation())
            .officialEmail(req.getOfficialEmail())
            .phonePrimary(req.getPhonePrimary())
            .build();
        branch.setBasicInfo(info);

        // ── Address ──────────────────────────────────────────────────────
        branch.setAddress(buildAddress(branch, req.getAddress()));

        // ── Default settings ─────────────────────────────────────────────
        branch.setSettings(buildSettings(branch, null));

        // ── Academic year ────────────────────────────────────────────────
        branch.getAcademicYears().add(buildDefaultAcademicYear(branch, req.getAcademic()));

        // ── Primary contact (from principal info) ────────────────────────
        if (req.getPrincipalName() != null && req.getPhonePrimary() != null) {
            SchoolOnboardRequest.ContactData contactData = new SchoolOnboardRequest.ContactData();
            contactData.setFullName(req.getPrincipalName());
            contactData.setDesignation(req.getPrincipalDesignation());
            contactData.setEmail(req.getOfficialEmail());
            contactData.setPhone(req.getPhonePrimary());
            branch.getContacts().add(buildContact(branch, contactData));
        }

        schoolRepository.save(branch);

        // ── Provision SCHOOL_ADMIN for the branch ────────────────────────
        String rawPassword    = generator.generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        String adminEmail     = resolveAdminEmail(req.getAdminEmail(), branch.getName());
        String adminFullName  = resolveAdminName(req.getAdminFullName(), req.getPrincipalName());

        Role adminRole = requireRole(SCHOOL_ADMIN_ROLE);
        SystemUser adminUser = SystemUser.builder()
            .email(adminEmail)
            .passwordHash(hashedPassword)
            .fullName(adminFullName)
            .phone(req.getAdminPhone())
            .userType(UserType.SCHOOL_ADMIN)
            .schoolId(branch.getId())
            .chainId(chainId)              // branch admin also carries chainId for scope
            .active(true)
            .mustChangePassword(true)
            .roles(Set.of(adminRole))
            .build();
        userRepository.save(adminUser);

        // ── Mark complete and activate ───────────────────────────────────
        branch.advanceOnboarding(OnboardingStep.COMPLETE);
        branch.setActive(true);
        schoolRepository.save(branch);
        logAudit(branch, OnboardingStep.COMPLETE, "COMPLETED");

        log.info("Branch [{}] '{}' onboarded under chain [{}], admin [{}]",
            branch.getId(), branch.getBranchCode(), chainId, adminEmail);

        return BranchOnboardResponse.builder()
            .schoolId(branch.getId())
            .schoolName(branch.getName())
            .branchCode(branch.getBranchCode())
            .branchName(branch.getBranchName())
            .headquarter(branch.isHeadquarter())
            .schoolType(branch.getType())
            .plan(branch.getPlan())
            .udiseCode(branch.getUdiseCode())
            .chainId(chainId)
            .chainName(chain.getName())
            .city(req.getAddress().getCity())
            .state(req.getAddress().getState())
            .pincode(req.getAddress().getPincode())
            .active(true)
            .onboardingComplete(true)
            .createdAt(LocalDateTime.now())
            .adminCredentials(SchoolOnboardResponse.AdminCredentials.builder()
                .userId(adminUser.getId())
                .email(adminEmail)
                .username(adminEmail)
                .temporaryPassword(rawPassword)
                .loginUrl(LOGIN_URL)
                .note("Temporary password — must be changed on first login.")
                .build())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP-BASED ONBOARDING (unchanged — preserved from original)
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public OnboardingResponse submitBasicInfo(BasicInfoRequest req) {

        if (req.getUdiseCode() != null
                && schoolRepository.existsByUdiseCodeAndDeletedFalse(req.getUdiseCode())) {
            throw new BadRequestException(
                "A school with UDISE code " + req.getUdiseCode() + " already exists.");
        }
        if (req.getChainId() != null && req.getBranchCode() != null
                && schoolRepository.existsByChainIdAndBranchCodeAndDeletedFalse(
                        req.getChainId(), req.getBranchCode())) {
            throw new BadRequestException(
                "Branch code " + req.getBranchCode() + " already used in this chain.");
        }

        SchoolChain chain = null;
        if (req.getChainId() != null) {
            chain = new SchoolChain();
            chain.setId(req.getChainId());
        }

        School school = School.builder()
            .chain(chain)
            .branchCode(req.getBranchCode())
            .branchName(req.getBranchName())
            .headquarter(req.isHeadquarter())
            .name(req.getName())
            .type(req.getType())
            .board(req.getBoard())
            .medium(req.getMedium())
            .udiseCode(req.getUdiseCode())
            .affiliationNo(req.getAffiliationNo())
            .onboardingStep(OnboardingStep.BASIC_INFO)
            .onboardingComplete(false)
            .active(false)
            .build();

        SchoolBasicInfo info = SchoolBasicInfo.builder()
            .school(school)
            .tagline(req.getTagline())
            .principalName(req.getPrincipalName())
            .principalDesignation(req.getPrincipalDesignation())
            .officialEmail(req.getOfficialEmail())
            .website(req.getWebsite())
            .phonePrimary(req.getPhonePrimary())
            .phoneSecondary(req.getPhoneSecondary())
            .establishedYear(req.getEstablishedYear())
            .foundedOn(req.getFoundedOn())
            .trustName(req.getTrustName())
            .managementType(req.getManagementType())
            .coEd(req.isCoEd())
            .residential(req.isResidential())
            .build();
        school.setBasicInfo(info);
        schoolRepository.save(school);

        OnboardingResponse.AdminCredentials credentials =
            adminProvisioning.provisionSchoolAdmin(
                school, req.getAdminEmail(), req.getAdminFullName(), req.getAdminPhone());

        school.advanceOnboarding(OnboardingStep.CONTACT);
        schoolRepository.save(school);
        logAudit(school, OnboardingStep.BASIC_INFO, "COMPLETED");

        return OnboardingResponse.builder()
            .schoolId(school.getId())
            .schoolName(school.getName())
            .currentStep(OnboardingStep.BASIC_INFO)
            .nextStep(OnboardingStep.CONTACT)
            .onboardingComplete(false)
            .adminCredentials(credentials)
            .message("School created. Admin account provisioned.")
            .build();
    }

    @Transactional
    public OnboardingResponse submitContacts(UUID schoolId, ContactRequest req) {

        School school = getSchoolAtStep(schoolId, OnboardingStep.CONTACT);
        school.getContacts().clear();

        long primaryCount = req.getContacts().stream()
                .filter(ContactRequest.ContactEntry::isPrimary).count();
        if (primaryCount > 1) {
            throw new BadRequestException("Only one contact can be marked as primary.");
        }

        req.getContacts().forEach(c -> school.getContacts().add(
            SchoolContact.builder()
                .school(school)
                .contactType(c.getContactType())
                .fullName(c.getFullName())
                .designation(c.getDesignation())
                .email(c.getEmail())
                .phone(c.getPhone())
                .phoneAlternate(c.getPhoneAlternate())
                .primary(c.isPrimary())
                .receiveNotifications(c.isReceiveNotifications())
                .build()));

        school.advanceOnboarding(OnboardingStep.ADDRESS);
        schoolRepository.save(school);
        logAudit(school, OnboardingStep.CONTACT, "COMPLETED");
        return stepResponse(school, OnboardingStep.CONTACT, OnboardingStep.ADDRESS, "Contacts saved.");
    }

    @Transactional
    public OnboardingResponse submitAddress(UUID schoolId, AddressRequest req) {

        School school = getSchoolAtStep(schoolId, OnboardingStep.ADDRESS);
        school.setAddress(SchoolAddress.builder()
            .school(school)
            .addressLine1(req.getAddressLine1())
            .addressLine2(req.getAddressLine2())
            .landmark(req.getLandmark())
            .city(req.getCity())
            .district(req.getDistrict())
            .state(req.getState())
            .pincode(req.getPincode())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .mapLink(req.getMapLink())
            .googlePlaceId(req.getGooglePlaceId())
            .directions(req.getDirections())
            .build());

        school.advanceOnboarding(OnboardingStep.ACADEMIC);
        schoolRepository.save(school);
        logAudit(school, OnboardingStep.ADDRESS, "COMPLETED");
        return stepResponse(school, OnboardingStep.ADDRESS, OnboardingStep.ACADEMIC, "Address saved.");
    }

    @Transactional
    public OnboardingResponse submitAcademicSetup(UUID schoolId, AcademicSetupRequest req) {

        School school = getSchoolAtStep(schoolId, OnboardingStep.ACADEMIC);

        if (!req.getAcademicYearEnd().isAfter(req.getAcademicYearStart())) {
            throw new BadRequestException("Academic year end date must be after start date.");
        }

        AcademicYear year = AcademicYear.builder()
            .school(school).label(req.getAcademicYearLabel())
            .startDate(req.getAcademicYearStart()).endDate(req.getAcademicYearEnd())
            .current(true).locked(false).build();

        req.getTerms().forEach(t -> year.getTerms().add(SchoolTerm.builder()
            .academicYear(year).name(t.getName()).sortOrder(t.getSortOrder())
            .startDate(t.getStartDate()).endDate(t.getEndDate()).build()));

        req.getShifts().forEach(s -> year.getShifts().add(SchoolShift.builder()
            .academicYear(year).name(s.getName())
            .startTime(s.getStartTime()).endTime(s.getEndTime())
            .defaultShift(s.isDefaultShift()).build()));

        school.getAcademicYears().add(year);
        school.getGradeRanges().clear();
        req.getGradeRanges().forEach(g -> school.getGradeRanges().add(SchoolGradeRange.builder()
            .school(school).segmentName(g.getSegmentName())
            .fromGrade(g.getFromGrade()).toGrade(g.getToGrade())
            .fromGradeOrder(g.getFromGradeOrder()).toGradeOrder(g.getToGradeOrder())
            .boardOverride(g.getBoardOverride()).build()));

        school.advanceOnboarding(OnboardingStep.DOCUMENTS);
        schoolRepository.save(school);
        logAudit(school, OnboardingStep.ACADEMIC, "COMPLETED");
        return stepResponse(school, OnboardingStep.ACADEMIC, OnboardingStep.DOCUMENTS,
            "Academic year, terms, shifts, and grade ranges saved.");
    }

    @Transactional
    public OnboardingResponse submitDocument(UUID schoolId, DocumentUploadRequest req) {

        School school = getSchoolAtStep(schoolId, OnboardingStep.DOCUMENTS);
        String name = (req.getDocumentName() != null && !req.getDocumentName().isBlank())
            ? req.getDocumentName() : req.getDocumentType().name().replace("_", " ");

        school.getDocuments().add(SchoolDocument.builder()
            .school(school).documentType(req.getDocumentType()).documentName(name)
            .fileUrl(req.getFileUrl()).mimeType(req.getMimeType())
            .fileSizeBytes(req.getFileSizeBytes()).originalFileName(req.getOriginalFileName())
            .verified(false).build());

        schoolRepository.save(school);
        return stepResponse(school, OnboardingStep.DOCUMENTS, OnboardingStep.DOCUMENTS,
            "Document '" + name + "' uploaded.");
    }

    @Transactional
    public OnboardingResponse completeOnboarding(UUID schoolId) {
        School school = schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));

        if (school.getOnboardingStep() != OnboardingStep.DOCUMENTS) {
            throw new BadRequestException(
                "Cannot complete onboarding at step " + school.getOnboardingStep());
        }

        school.advanceOnboarding(OnboardingStep.COMPLETE);
        school.setActive(true);
        schoolRepository.save(school);
        logAudit(school, OnboardingStep.COMPLETE, "COMPLETED");

        return OnboardingResponse.builder()
            .schoolId(school.getId()).schoolName(school.getName())
            .currentStep(OnboardingStep.COMPLETE).nextStep(null)
            .onboardingComplete(true)
            .message("Onboarding complete! School is now active.")
            .build();
    }

    @Transactional(readOnly = true)
    public OnboardingResponse getStatus(UUID schoolId) {
        School school = schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        OnboardingStep current = school.getOnboardingStep();
        return OnboardingResponse.builder()
            .schoolId(school.getId()).schoolName(school.getName())
            .currentStep(current).nextStep(nextStep(current))
            .onboardingComplete(school.isOnboardingComplete())
            .message("School is at step: " + current)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private SchoolAddress buildAddress(School school,
                                       SchoolOnboardRequest.AddressData a) {
        return SchoolAddress.builder()
            .school(school)
            .addressLine1(a.getAddressLine1())
            .addressLine2(a.getAddressLine2())
            .landmark(a.getLandmark())
            .city(a.getCity())
            .district(a.getDistrict())
            .state(a.getState())
            .pincode(a.getPincode())
            .latitude(a.getLatitude())
            .longitude(a.getLongitude())
            .mapLink(a.getMapLink())
            .googlePlaceId(a.getGooglePlaceId())
            .directions(a.getDirections())
            .build();
    }

    private SchoolContact buildContact(School school,
                                       SchoolOnboardRequest.ContactData c) {
        return SchoolContact.builder()
            .school(school)
            .contactType(c.getContactType() != null
                ? c.getContactType() : ContactType.PRINCIPAL)
            .fullName(c.getFullName())
            .designation(c.getDesignation())
            .email(c.getEmail())
            .phone(c.getPhone())
            .primary(true)
            .receiveNotifications(c.isReceiveNotifications())
            .build();
    }

    private SchoolSettings buildSettings(School school,
                                          SchoolOnboardRequest.SettingsData s) {
        SchoolSettings.SchoolSettingsBuilder b = SchoolSettings.builder().school(school);
        if (s != null) {
            b.locale(s.getLocale())
             .timezone(s.getTimezone())
             .academicYearStartMonth(s.getAcademicYearStartMonth())
             .minAttendancePct(s.getMinAttendancePct())
             .saturdayWorking(s.isSaturdayWorking())
             .gstApplicable(s.isGstApplicable())
             .gstin(s.getGstin())
             .smsEnabled(s.isSmsEnabled())
             .whatsappEnabled(s.isWhatsappEnabled())
             .brandColorPrimary(s.getBrandColorPrimary())
             .brandColorSecondary(s.getBrandColorSecondary());
        }
        return b.build();
    }

    /**
     * Builds a sensible default academic year.
     * If req is null or incomplete, falls back to the current Indian
     * academic year (April 1 – March 31).
     */
    private AcademicYear buildDefaultAcademicYear(School school,
                                                   SchoolOnboardRequest.AcademicData req) {
        String label;
        LocalDate start;
        LocalDate end;

        if (req != null && req.getLabel() != null && req.getStartDate() != null) {
            label = req.getLabel();
            start = req.getStartDate();
            end   = req.getEndDate() != null ? req.getEndDate() : start.plusYears(1).minusDays(1);
        } else {
            // Default: current Indian academic year
            int startYear = LocalDate.now().getMonthValue() >= 4
                ? LocalDate.now().getYear()
                : LocalDate.now().getYear() - 1;
            label = startYear + "-" + String.format("%02d", (startYear + 1) % 100);
            start = LocalDate.of(startYear, 4, 1);
            end   = LocalDate.of(startYear + 1, 3, 31);
        }

        AcademicYear year = AcademicYear.builder()
            .school(school).label(label)
            .startDate(start).endDate(end)
            .current(true).locked(false).build();

        // Default: two terms
        year.getTerms().add(SchoolTerm.builder()
            .academicYear(year).name("Term 1").sortOrder(1)
            .startDate(start).endDate(start.withMonth(9).withDayOfMonth(30)).build());
        year.getTerms().add(SchoolTerm.builder()
            .academicYear(year).name("Term 2").sortOrder(2)
            .startDate(start.withMonth(10).withDayOfMonth(1)).endDate(end).build());

        // Default: one morning shift
        year.getShifts().add(SchoolShift.builder()
            .academicYear(year).name("Morning Shift")
            .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(14, 0))
            .defaultShift(true).build());

        return year;
    }

    private Role requireRole(String roleName) {
        return roleRepository.findSystemRoleByName(roleName)
            .orElseThrow(() -> new IllegalStateException(
                "System role '" + roleName + "' not found — ensure V2 migration has run."));
    }

    private String resolveAdminEmail(String override, String schoolName) {
        String email = (override != null && !override.isBlank())
            ? override.trim().toLowerCase()
            : generator.generateAdminEmail(schoolName);
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            email = generator.generateAdminEmail(
                schoolName + " " + System.currentTimeMillis() % 10000);
        }
        return email;
    }

    private String resolveAdminName(String override, String principalName) {
        if (override != null && !override.isBlank()) return override.trim();
        if (principalName != null && !principalName.isBlank()) return principalName.trim();
        return "School Admin";
    }

    private School getSchoolAtStep(UUID schoolId, OnboardingStep expectedStep) {
        School school = schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        if (school.getOnboardingStep() != expectedStep) {
            throw new BadRequestException(
                "Expected step " + expectedStep + " but school is at " +
                school.getOnboardingStep());
        }
        return school;
    }

    private OnboardingResponse stepResponse(School school, OnboardingStep completed,
                                            OnboardingStep next, String message) {
        return OnboardingResponse.builder()
            .schoolId(school.getId()).schoolName(school.getName())
            .currentStep(completed).nextStep(next)
            .onboardingComplete(school.isOnboardingComplete())
            .message(message).build();
    }

    private void logAudit(School school, OnboardingStep step, String action) {
        UUID performedBy = TenantContext.getCurrentTenant() != null
            ? TenantContext.getCurrentTenant()
            : UUID.fromString("00000000-0000-0000-0000-000000000000");
        auditRepository.save(SchoolOnboardingAudit.builder()
            .school(school).step(step).action(action).performedBy(performedBy).build());
    }

    private OnboardingStep nextStep(OnboardingStep current) {
        return switch (current) {
            case BASIC_INFO -> OnboardingStep.CONTACT;
            case CONTACT    -> OnboardingStep.ADDRESS;
            case ADDRESS    -> OnboardingStep.ACADEMIC;
            case ACADEMIC   -> OnboardingStep.DOCUMENTS;
            case DOCUMENTS  -> OnboardingStep.COMPLETE;
            case COMPLETE   -> null;
        };
    }
}
