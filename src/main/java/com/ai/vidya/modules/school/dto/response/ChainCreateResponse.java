package com.ai.vidya.modules.school.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response from POST /api/v1/chains
 *
 * Contains the new chain's ID plus the one-time CHAIN_ADMIN credentials.
 */
@Data
@Builder
public class ChainCreateResponse {

    // ── Chain identity ─────────────────────────────────────────────────────
    private UUID   chainId;
    private String chainName;
    private String chainCode;
    private String website;
    private boolean active;

    // ── Timestamps ─────────────────────────────────────────────────────────
    private LocalDateTime createdAt;

    // ── Chain Admin account (one-time) ─────────────────────────────────────
    private ChainAdminCredentials chainAdminCredentials;

    @Data
    @Builder
    public static class ChainAdminCredentials {
        private UUID   userId;
        private String email;
        private String username;
        private String temporaryPassword;
        private String loginUrl;
        private String note;
    }
}
