package com.ai.vidya.modules.school.service;

import com.ai.vidya.common.enums.UserType;
import com.ai.vidya.modules.school.entity.School;
import com.ai.vidya.modules.school.dto.response.OnboardingResponse.AdminCredentials;
import com.ai.vidya.modules.user.entity.Role;
import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.RoleRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates and provisions the SCHOOL_ADMIN SystemUser account
 * whenever a new school is onboarded (form or bulk upload).
 *
 * Responsibilities:
 *   1. Generate or accept admin email / name
 *   2. Generate a secure temporary password
 *   3. Hash the password (BCrypt)
 *   4. Create the SystemUser row with mustChangePassword = true
 *   5. Assign the SCHOOL_ADMIN role
 *   6. Return the plain-text temp password (shown once to the platform admin)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolAdminProvisioningService {

    private static final String SCHOOL_ADMIN_ROLE  = "SCHOOL_ADMIN";
    private static final String DEFAULT_LOGIN_URL   = "https://app.vidya.ai/login";

    private final SystemUserRepository          userRepository;
    private final RoleRepository                roleRepository;
    private final PasswordEncoder               passwordEncoder;
    private final UsernamePasswordGeneratorService generator;

    /**
     * Creates the school admin account for a newly onboarded school.
     *
     * @param school        the persisted School entity
     * @param adminEmail    optional override (null = auto-generate)
     * @param adminFullName optional override (null = "School Admin")
     * @param adminPhone    optional phone number
     * @return              AdminCredentials containing the one-time temp password
     */
    @Transactional
    public AdminCredentials provisionSchoolAdmin(
            School school,
            String adminEmail,
            String adminFullName,
            String adminPhone
    ) {
        // 1 ── Resolve email
        String email = (adminEmail != null && !adminEmail.isBlank())
            ? adminEmail.trim().toLowerCase()
            : generator.generateAdminEmail(school.getName());

        // Sanity check — email must be unique
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            log.warn("Admin email {} already exists, generating fallback for school {}",
                email, school.getId());
            email = generator.generateAdminEmail(school.getName() + " " + school.getId().toString().substring(0, 4));
        }

        // 2 ── Resolve full name
        String fullName = resolveAdminName(adminFullName, school);

        // 3 ── Generate + hash temporary password
        String rawPassword    = generator.generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // 4 ── Assign role
        Role schoolAdminRole = roleRepository.findSystemRoleByName(SCHOOL_ADMIN_ROLE)
            .orElseThrow(() -> new IllegalStateException(
                "SCHOOL_ADMIN role not found in DB — run seed data migration first."));

        // 5 ── Persist SystemUser
        SystemUser admin = SystemUser.builder()
            .email(email)
            .passwordHash(hashedPassword)
            .fullName(fullName)
            .phone(adminPhone)
            .userType(UserType.SCHOOL_ADMIN)
            .schoolId(school.getId())
            .chainId(school.getChain() != null ? school.getChain().getId() : null)
            .active(true)
            .mustChangePassword(true)   // force reset on first login
            .roles(Set.of(schoolAdminRole))
            .build();

        userRepository.save(admin);
        log.info("Provisioned SCHOOL_ADMIN [{}] for school [{}]", email, school.getId());

        // 6 ── Return plain-text credentials (stored nowhere — shown once)
        return AdminCredentials.builder()
            .email(email)
            .username(email)
            .temporaryPassword(rawPassword)
            .loginUrl(DEFAULT_LOGIN_URL)
            .note("This password is temporary and must be changed on first login.")
            .build();
    }

    private String resolveAdminName(String adminFullName, School school) {
        if (adminFullName != null && !adminFullName.isBlank()) {
            return adminFullName.trim();
        }
        // Try to use basic info principal name if available
        if (school.getBasicInfo() != null
                && school.getBasicInfo().getPrincipalName() != null
                && !school.getBasicInfo().getPrincipalName().isBlank()) {
            return school.getBasicInfo().getPrincipalName();
        }
        return "School Admin";
    }
}
