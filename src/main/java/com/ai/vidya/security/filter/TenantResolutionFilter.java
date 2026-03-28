package com.ai.vidya.security.filter;

import com.ai.vidya.security.jwt.JwtTokenProvider;
import com.ai.vidya.tenant.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Servlet filter that runs once per request to:
 * <ol>
 *   <li>Extract the Bearer JWT from the Authorization header.</li>
 *   <li>Validate the token.</li>
 *   <li>Set {@code tenantId} and {@code schoolId} in {@link TenantContext}.</li>
 *   <li>Build a Spring Security {@link UsernamePasswordAuthenticationToken}
 *       carrying all permissions as {@link SimpleGrantedAuthority} objects.</li>
 *   <li>Clear the {@link TenantContext} in a {@code finally} block.</li>
 * </ol>
 *
 * <p>Fall-through strategy: if the JWT is missing or invalid the filter chain
 * continues without setting an authentication; Spring Security will reject
 * the request at the authorization stage with 401.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.tenant.header:X-Tenant-Id}")
    private String tenantHeader;

    @Value("${app.tenant.school-header:X-School-Id}")
    private String schoolHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                // ── 1. Resolve tenant & school ────────────────────────────
                UUID tenantId = resolveUuid(token, request, tenantHeader, "tenantId",
                        () -> jwtTokenProvider.extractTenantId(token));
                UUID schoolId = resolveUuid(token, request, schoolHeader, "schoolId",
                        () -> jwtTokenProvider.extractSchoolId(token));

                if (tenantId != null && schoolId != null) {
                    TenantContext.setTenantId(tenantId);
                    TenantContext.setSchoolId(schoolId);
                    log.debug("TenantResolutionFilter: tenantId={} schoolId={}", tenantId, schoolId);
                }

                // ── 2. Build Spring Security principal ────────────────────
                List<String> permissions = jwtTokenProvider.extractPermissions(token);
                List<SimpleGrantedAuthority> authorities = permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // Principal = email string for simplicity; swap with a UserDetails impl if needed
                String principal = String.valueOf(jwtTokenProvider.extractUserId(token));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(new JwtAuthDetails(tenantId, schoolId, permissions));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear to prevent ThreadLocal leakage in pooled threads
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * UUID resolution priority:
     * 1. JWT claim (most authoritative — server-signed)
     * 2. HTTP header (useful for super-admin / proxy scenarios)
     */
    private UUID resolveUuid(String token, HttpServletRequest request,
                              String headerName, String claimName,
                              java.util.function.Supplier<UUID> jwtExtractor) {
        try {
            return jwtExtractor.get();
        } catch (Exception e) {
            // Fall back to header
            String headerValue = request.getHeader(headerName);
            if (StringUtils.hasText(headerValue)) {
                try {
                    return UUID.fromString(headerValue);
                } catch (IllegalArgumentException ignored) {
                    log.warn("Invalid UUID in header {}: {}", headerName, headerValue);
                }
            }
            log.debug("Could not resolve {} from JWT or header", claimName);
            return null;
        }
    }

    /**
     * Extra details attached to the authentication token — accessible via
     * {@code ((JwtAuthDetails) SecurityContextHolder.getContext()
     *          .getAuthentication().getDetails())}.
     */
    public record JwtAuthDetails(UUID tenantId, UUID schoolId, List<String> permissions) {}
}
