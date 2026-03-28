package com.ai.vidya.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed binding for all vidyaos.* properties in application.yml.
 *
 * Example configuration:
 *
 * vidyaos:
 *   jwt:
 *     secret: ${JWT_SECRET}
 *     access-expiry-minutes: 60
 *     refresh-expiry-days: 7
 *   cors:
 *     allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
 *   platform-admins:
 *     - email:    ${SUPER_ADMIN_EMAIL:superadmin@vidya.ai}
 *       password: ${SUPER_ADMIN_PASSWORD}
 *       full-name: Super Admin
 *       role:     SUPER_ADMIN
 *     - email:    ${ADMIN_EMAIL:admin@vidya.ai}
 *       password: ${ADMIN_PASSWORD}
 *       full-name: Platform Admin
 *       role:     PLATFORM_ADMIN
 */
@Component
@ConfigurationProperties(prefix = "vidyaos")
@Validated
@Getter
@Setter
public class VidyaOsProperties {

    // ── JWT ──────────────────────────────────────────────────────────────────

    @Valid
    private Jwt jwt = new Jwt();

    @Getter @Setter
    public static class Jwt {
        @NotBlank
        private String secret;
        private long   accessExpiryMinutes  = 60;
        private int    refreshExpiryDays    = 7;
    }

    // ── CORS ─────────────────────────────────────────────────────────────────

    @Valid
    private Cors cors = new Cors();

    @Getter @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    // ── Platform admins ───────────────────────────────────────────────────────

    /**
     * List of platform-team users to seed on startup.
     * Each entry creates a SystemUser with the given role if it doesn't exist.
     * Safe to run on every startup — checks by email before inserting.
     */
    @Valid
    private List<PlatformAdmin> platformAdmins = new ArrayList<>();

    @Getter @Setter
    public static class PlatformAdmin {

        @NotBlank(message = "Platform admin email is required")
        @Email(message = "Platform admin email must be valid")
        private String email;

        /**
         * Plain-text password from environment variable.
         * Hashed with BCrypt before storage — never stored as plaintext.
         * Minimum 12 characters enforced here; enforce complexity in your
         * environment variable management.
         */
        @NotBlank(message = "Platform admin password is required")
        @Size(min = 12, message = "Platform admin password must be at least 12 characters")
        private String password;

        @NotBlank(message = "Platform admin full name is required")
        @Size(max = 150)
        private String fullName;

        /**
         * Must match an existing role name in the roles table.
         * Typically: SUPER_ADMIN or PLATFORM_ADMIN
         */
        @NotBlank(message = "Platform admin role is required")
        private String role;

        /**
         * Optional phone number.
         */
        private String phone;
    }
}