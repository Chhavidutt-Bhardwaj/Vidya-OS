package com.ai.vidya.security;

import com.ai.vidya.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService              jwtService;
    private final VidyaUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID   userId      = jwtService.extractUserId(token);
            UUID   schoolId    = jwtService.extractSchoolId(token);
            UUID   chainId     = jwtService.extractChainId(token);
            String primaryRole = jwtService.extractPrimaryRole(token);

            // Populate TenantContext for this thread BEFORE the request proceeds
            if (schoolId != null) TenantContext.setCurrentTenant(schoolId);
            if (chainId  != null) TenantContext.setCurrentChain(chainId);
            if (primaryRole != null) TenantContext.setCurrentRole(primaryRole);

            // Load user from cache (Redis) — no DB hit on every request
            VidyaUserDetails userDetails = userDetailsService.loadUserById(userId);

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception ex) {
            log.warn("JWT processing failed: {}", ex.getMessage());
            // Don't throw — let Spring Security reject in the next filter
        } finally {
            // CRITICAL: always clear ThreadLocal to prevent leaks in thread pools
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }
    }
}