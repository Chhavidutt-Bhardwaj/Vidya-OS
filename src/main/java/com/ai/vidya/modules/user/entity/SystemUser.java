package com.ai.vidya.modules.user.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Platform user — every human who logs in has a SystemUser row.
 *
 * User types and their scope:
 *   SUPER_ADMIN   → schoolId=null,  chainId=null   (full platform access)
 *   CHAIN_ADMIN   → schoolId=null,  chainId=SET    (all branches of that chain)
 *   SCHOOL_ADMIN  → schoolId=SET,   chainId=null   (one school only)
 *   PRINCIPAL     → schoolId=SET,   chainId=null
 *   TEACHER       → schoolId=SET,   chainId=null
 *   PARENT        → schoolId=SET,   chainId=null
 *
 * Account locking:
 *   After MAX_FAILED_ATTEMPTS consecutive failed logins, lockedUntil is set
 *   to NOW + LOCK_DURATION_MINUTES. isLocked() checks this field.
 *   On successful login, failedLoginAttempts is reset to 0 and lockedUntil cleared.
 */
@Entity
@Table(
    name = "system_users",
    indexes = {
        @Index(name = "idx_user_email",     columnList = "email",     unique = true),
        @Index(name = "idx_user_school_id", columnList = "school_id"),
        @Index(name = "idx_user_chain_id",  columnList = "chain_id"),
        @Index(name = "idx_user_type",      columnList = "user_type"),
        @Index(name = "idx_user_active",    columnList = "active"),
        @Index(name = "idx_user_locked",    columnList = "locked_until")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemUser extends BaseEntity {

    // ── Identity ────────────────────────────────────────────────────────────

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt-hashed password — never store plaintext */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    // ── Tenant scope ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    /** NULL for SUPER_ADMIN and CHAIN_ADMIN */
    @Column(name = "school_id")
    private UUID schoolId;

    /** NULL unless CHAIN_ADMIN or a branch SCHOOL_ADMIN */
    @Column(name = "chain_id")
    private UUID chainId;

    // ── Status ───────────────────────────────────────────────────────────────

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Forces password reset on next login — set true for auto-provisioned accounts */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // ── Account locking (brute-force protection) ──────────────────────────────

    /**
     * Number of consecutive failed login attempts since the last successful login.
     * Reset to 0 on any successful login.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /**
     * When non-null, the account is locked until this timestamp.
     * Set by AuthService.handleFailedAttempt() when failedLoginAttempts
     * reaches the configured threshold.
     * Cleared (set to null) on successful login.
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // ── Roles ────────────────────────────────────────────────────────────────

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name               = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ── Business logic helpers ───────────────────────────────────────────────

    /**
     * Returns true if the account is currently locked.
     * A lock expires automatically when lockedUntil passes — no cron job needed.
     */
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Returns true if this account can be used to log in right now.
     * Checks: active flag, soft-delete flag, and lock window.
     */
    public boolean isActive() {
        return active && !Boolean.TRUE.equals(getDeleted()) && !isLocked();
    }

    /**
     * Records a successful login — resets lock state and timestamps last login.
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt         = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil         = null;
    }

    /**
     * Kept for backward compatibility with existing call sites.
     * Delegates to recordSuccessfulLogin().
     */
    public void recordLogin() {
        recordSuccessfulLogin();
    }
}
