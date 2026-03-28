package com.ai.vidya.modules.user.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_rt_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_rt_user_id",    columnList = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Stored as SHA-256 hash of the actual token — never store raw tokens */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /** Device/browser fingerprint for multi-device refresh tracking */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
