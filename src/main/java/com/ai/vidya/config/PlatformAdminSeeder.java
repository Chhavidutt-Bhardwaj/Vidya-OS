package com.ai.vidya.config;

import com.ai.vidya.common.enums.UserType;
import com.ai.vidya.modules.user.entity.Role;
import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.RoleRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds platform-team admin users on every startup.
 *
 * Runs AFTER Flyway migrations complete (Flyway is guaranteed to run before
 * any ApplicationRunner because Spring Boot's FlywayAutoConfiguration is
 * triggered during the context refresh, before ApplicationRunners are called).
 *
 * IDEMPOTENT — skips any email that already exists in system_users.
 * Safe to run in production restarts.
 *
 * Configure in application.yml:
 *
 *   vidyaos:
 *     platform-admins:
 *       - email:     ${SUPER_ADMIN_EMAIL:superadmin@vidya.ai}
 *         password:  ${SUPER_ADMIN_PASSWORD}         # env var — never hardcode
 *         full-name: Super Admin
 *         role:      SUPER_ADMIN
 *       - email:     ${PLATFORM_ADMIN_EMAIL:admin@vidya.ai}
 *         password:  ${PLATFORM_ADMIN_PASSWORD}
 *         full-name: Platform Admin
 *         role:      PLATFORM_ADMIN
 *
 * Roles (SUPER_ADMIN, PLATFORM_ADMIN) must already exist in the roles table —
 * they are seeded by V2 and V4 Flyway migrations respectively.
 *
 * Security notes:
 *   - mustChangePassword = false for platform team (they manage their own passwords)
 *   - Passwords are BCrypt-hashed before storage — never stored as plaintext
 *   - Existing users are NEVER updated — this seeder only creates missing users
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminSeeder implements ApplicationRunner {

    private final VidyaOsProperties    properties;
    private final SystemUserRepository userRepository;
    private final RoleRepository       roleRepository;
    private final PasswordEncoder      passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        var admins = properties.getPlatformAdmins();

        if (admins == null || admins.isEmpty()) {
            log.warn("PlatformAdminSeeder: no platform-admins configured under " +
                     "vidyaos.platform-admins — skipping. " +
                     "Configure at least one SUPER_ADMIN before going to production.");
            return;
        }

        int created = 0;
        int skipped = 0;

        for (VidyaOsProperties.PlatformAdmin adminConfig : admins) {

            // ── Skip if email already in DB ──────────────────────────────────
            if (userRepository.existsByEmailAndDeletedFalse(adminConfig.getEmail())) {
                log.debug("PlatformAdminSeeder: [{}] already exists — skipping",
                    adminConfig.getEmail());
                skipped++;
                continue;
            }

            // ── Resolve role ─────────────────────────────────────────────────
            Role role = roleRepository.findByName(adminConfig.getRole())
                .orElseThrow(() -> new IllegalStateException(
                    "PlatformAdminSeeder: role '" + adminConfig.getRole() + "' not found. " +
                    "Ensure V2 (SUPER_ADMIN) or V4 (PLATFORM_ADMIN) migration has run."));

            // ── Resolve UserType from role name ──────────────────────────────
            UserType userType = resolveUserType(adminConfig.getRole());

            // ── Hash password ────────────────────────────────────────────────
            String hashedPassword = passwordEncoder.encode(adminConfig.getPassword());

            // ── Create user ──────────────────────────────────────────────────
            SystemUser user = SystemUser.builder()
                .email(adminConfig.getEmail().trim().toLowerCase())
                .passwordHash(hashedPassword)
                .fullName(adminConfig.getFullName())
                .phone(adminConfig.getPhone())
                .userType(userType)
                .schoolId(null)         // platform team — no school scope
                .chainId(null)          // platform team — no chain scope
                .active(true)
                .mustChangePassword(false)   // platform team manage their own passwords
                .failedLoginAttempts(0)
                .roles(Set.of(role))
                .build();

            userRepository.save(user);
            created++;

            // Log email but never the password
            log.info("PlatformAdminSeeder: created [{}] user — email={} role={}",
                userType, adminConfig.getEmail(), adminConfig.getRole());
        }

        log.info("PlatformAdminSeeder complete: {} created, {} already existed",
            created, skipped);
    }

    /**
     * Maps a role name to the appropriate UserType enum value.
     * Both SUPER_ADMIN and PLATFORM_ADMIN are platform-scoped (no school/chain).
     */
    private UserType resolveUserType(String roleName) {
        return switch (roleName.toUpperCase()) {
            case "SUPER_ADMIN"     -> UserType.SUPER_ADMIN;
            case "PLATFORM_ADMIN"  -> UserType.SUPER_ADMIN;  // same scope, narrower permissions
            default -> throw new IllegalStateException(
                "PlatformAdminSeeder: role '" + roleName + "' is not a valid platform role. " +
                "Allowed values: SUPER_ADMIN, PLATFORM_ADMIN");
        };
    }
}