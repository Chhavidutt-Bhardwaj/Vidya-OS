package com.ai.vidya.modules.school.dto.response;

import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.enums.SchoolType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response from POST /api/v1/chains/{chainId}/branches
 *
 * Returns the new branch's identity, its place in the chain,
 * and the one-time SCHOOL_ADMIN credentials.
 */
@Data
@Builder
public class BranchOnboardResponse {

    // ── Branch identity ────────────────────────────────────────────────────
    private UUID       schoolId;
    private String     schoolName;
    private String     branchCode;
    private String     branchName;
    private boolean    headquarter;
    private SchoolType schoolType;
    private PlanType   plan;
    private String     udiseCode;

    // ── Chain reference ────────────────────────────────────────────────────
    private UUID   chainId;
    private String chainName;

    // ── Address summary ────────────────────────────────────────────────────
    private String city;
    private String state;
    private String pincode;

    // ── Status ─────────────────────────────────────────────────────────────
    private boolean        active;
    private boolean        onboardingComplete;
    private LocalDateTime  createdAt;

    // ── Admin account (one-time) ───────────────────────────────────────────
    private SchoolOnboardResponse.AdminCredentials adminCredentials;
}
