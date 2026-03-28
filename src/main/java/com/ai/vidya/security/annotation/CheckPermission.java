package com.ai.vidya.security.annotation;

import java.lang.annotation.*;

/**
 * Method-level annotation to declaratively enforce RBAC.
 *
 * <p>Usage:
 * <pre>
 *   {@literal @}CheckPermission("STAFF:CREATE")
 *   public StaffResponse createStaff(CreateStaffRequest request) { ... }
 * </pre>
 *
 * <p>The corresponding AOP advice ({@link com.ai.vidya.security.aspect.PermissionCheckAspect})
 * extracts the current principal's permissions from the Spring Security context and
 * throws {@link com.ai.vidya.common.exception.AccessDeniedException} if the required
 * permission is absent.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckPermission {

    /**
     * The permission code that must be present in the JWT's {@code permissions} claim.
     * Format: {@code MODULE:ACTION}, e.g. {@code STAFF:CREATE}.
     */
    String value();
}
