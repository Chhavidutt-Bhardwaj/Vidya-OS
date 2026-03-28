package com.ai.vidya.modules.school.dto.response;

import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.enums.SchoolType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response from POST /api/v1/schools (standalone school onboarding).
 *
 * Contains all key identifiers the caller needs to reference this school
 * in subsequent API calls, plus the one-time admin credentials.
 */
@Data
@Builder
public class SchoolOnboardResponse {

    // ── School identity ────────────────────────────────────────────────────
    private UUID       schoolId;
    private String     schoolName;
    private SchoolType schoolType;
    private PlanType   plan;
    private String     udiseCode;

    // ── Address summary ────────────────────────────────────────────────────
    private String city;
    private String state;
    private String pincode;

    // ── Status ─────────────────────────────────────────────────────────────
    private boolean active;
    private boolean onboardingComplete;

    // ── Timestamps ─────────────────────────────────────────────────────────
    private LocalDateTime createdAt;

    // ── Admin account (one-time — show and store securely) ─────────────────
    private AdminCredentials adminCredentials;

    @Data
    @Builder
    public static class AdminCredentials {
        private UUID   userId;
        private String email;
        private String username;            // same as email
        private String temporaryPassword;   // shown once — BCrypt hash stored in DB
        private String loginUrl;
        private String note;
    }
}
