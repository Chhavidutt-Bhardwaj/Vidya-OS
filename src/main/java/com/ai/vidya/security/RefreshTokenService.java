package com.ai.vidya.security;

import com.ai.vidya.exception.AuthException;
import com.ai.vidya.modules.user.entity.RefreshToken;
import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.RefreshTokenRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepo;
    private final SystemUserRepository userRepo;
    private final JwtService             jwtService;

    @Value("${vidyaos.jwt.refresh-expiry-days:7}")
    private int refreshExpiryDays;

    @Transactional
    public String createRefreshToken(SystemUser user) {
        // Raw token = UUID (never stored — only SHA-256 hash stored in DB)
        String rawToken  = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusDays(refreshExpiryDays))
            .revoked(false)
            .build();

        refreshTokenRepo.save(refreshToken);
        return rawToken;  // return raw — client stores this
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);

        RefreshToken existing = refreshTokenRepo.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (!existing.isValid()) {
            // Token reuse detected — revoke all tokens for this user (security measure)
            refreshTokenRepo.revokeAllForUser(existing.getUserId());
            throw new AuthException("Refresh token expired or revoked. Please login again.");
        }

        // Revoke old token (rotation — one-time use)
        existing.setRevoked(true);
        refreshTokenRepo.save(existing);

        // Issue new refresh token
        SystemUser user = userRepo.findByIdAndDeletedFalse(existing.getUserId())
            .orElseThrow(() -> new AuthException("User not found"));

        return createRefreshToken(user);
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        refreshTokenRepo.findByTokenHash(tokenHash)
            .ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepo.save(t);
            });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepo.revokeAllForUser(userId);
    }

    public UUID getUserIdFromRawToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        return refreshTokenRepo.findByTokenHash(tokenHash)
            .filter(RefreshToken::isValid)
            .map(RefreshToken::getUserId)
            .orElseThrow(() -> new AuthException("Invalid or expired refresh token"));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}