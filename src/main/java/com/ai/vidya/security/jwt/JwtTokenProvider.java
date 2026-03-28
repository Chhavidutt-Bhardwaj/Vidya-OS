package com.ai.vidya.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Stateless JWT utility.
 *
 * <p>Expected JWT claims:
 * <pre>
 * {
 *   "sub"         : "user-uuid",
 *   "email"       : "user@school.com",
 *   "tenantId"    : "uuid",
 *   "schoolId"    : "uuid",
 *   "roles"       : ["TEACHER", "PRINCIPAL"],
 *   "permissions" : ["STAFF:READ", "TEACHER:PERFORMANCE_VIEW"]
 * }
 * </pre>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey   = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ── Token creation ────────────────────────────────────────────────────

    public String generateToken(UUID userId, String email, UUID tenantId, UUID schoolId,
                                List<String> roles, List<String> permissions) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email",       email)
                .claim("tenantId",    tenantId.toString())
                .claim("schoolId",    schoolId.toString())
                .claim("roles",       roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // ── Claim extraction ──────────────────────────────────────────────────

    public Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseAllClaims(token).getSubject());
    }

    public UUID extractTenantId(String token) {
        return UUID.fromString((String) parseAllClaims(token).get("tenantId"));
    }

    public UUID extractSchoolId(String token) {
        return UUID.fromString((String) parseAllClaims(token).get("schoolId"));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object perms = parseAllClaims(token).get("permissions");
        return perms instanceof List<?> list ? (List<String>) list : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseAllClaims(token).get("roles");
        return roles instanceof List<?> list ? (List<String>) list : Collections.emptyList();
    }

    // ── Validation ────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }
}
