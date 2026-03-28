package com.ai.vidya.security;

import com.ai.vidya.modules.user.entity.Role;
import com.ai.vidya.modules.user.entity.SystemUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    @Value("${vidyaos.jwt.secret}")
    private String jwtSecret;

    @Value("${vidyaos.jwt.access-expiry-minutes:60}")
    private long accessExpiryMinutes;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // ── Token generation ──────────────────────────────────────────────────

    public String generateAccessToken(SystemUser user) {
        Map<String, Object> claims = new HashMap<>();

        // schoolId & chainId encoded directly — JwtAuthFilter reads these
        // to populate TenantContext without a DB hit
        if (user.getSchoolId() != null) {
            claims.put("schoolId", user.getSchoolId().toString());
        }
        if (user.getChainId() != null) {
            claims.put("chainId", user.getChainId().toString());
        }

        claims.put("userType", user.getUserType().name());
        claims.put("email",    user.getEmail());

        Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
        claims.put("roles", roles);

        // Primary role for TenantContext.currentRole fast-path
        String primaryRole = roles.isEmpty() ? "UNKNOWN" : roles.iterator().next();
        claims.put("primaryRole", primaryRole);

        return Jwts.builder()
            .claims(claims)
            .subject(user.getId().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpiryMinutes * 60_000L))
            .signWith(signingKey())
            .compact();
    }

    // ── Claims extraction ─────────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public UUID extractSchoolId(String token) {
        Object val = extractAllClaims(token).get("schoolId");
        return val != null ? UUID.fromString(val.toString()) : null;
    }

    public UUID extractChainId(String token) {
        Object val = extractAllClaims(token).get("chainId");
        return val != null ? UUID.fromString(val.toString()) : null;
    }

    public String extractPrimaryRole(String token) {
        Object val = extractAllClaims(token).get("primaryRole");
        return val != null ? val.toString() : null;
    }

    // ── Validation ────────────────────────────────────────────────────────

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessExpirySeconds() {
        return accessExpiryMinutes * 60;
    }
}