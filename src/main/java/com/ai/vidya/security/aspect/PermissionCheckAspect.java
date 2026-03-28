package com.ai.vidya.security.aspect;

import com.ai.vidya.security.annotation.CheckPermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * AOP advice that fires before any method annotated with {@link CheckPermission}.
 *
 * <p>Flow:
 * <ol>
 *   <li>Reads the required permission from the annotation's {@code value()}.</li>
 *   <li>Fetches the current {@link Authentication} from the Security context.</li>
 *   <li>Checks whether the authentication's authorities contain the required permission.</li>
 *   <li>Throws {@link org.springframework.security.access.AccessDeniedException} if not found.</li>
 * </ol>
 */
@Aspect
@Component
@Slf4j
public class PermissionCheckAspect {

    @Before("@annotation(com.ai.vidya.security.annotation.CheckPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // ── 1. Extract the required permission from the annotation ────────
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        CheckPermission annotation = signature.getMethod().getAnnotation(CheckPermission.class);
        String required = annotation.value();

        // ── 2. Get the current authentication ────────────────────────────
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("PermissionCheckAspect: unauthenticated access attempt to {}", signature.getName());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication required");
        }

        // ── 3. Check authorities ──────────────────────────────────────────
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean hasPermission = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(required::equals);

        if (!hasPermission) {
            log.warn("PermissionCheckAspect: principal '{}' is missing permission '{}' " +
                    "required by '{}'", auth.getName(), required, signature.getName());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Missing required permission: " + required);
        }

        log.debug("PermissionCheckAspect: permission '{}' granted to '{}'", required, auth.getName());
    }
}
