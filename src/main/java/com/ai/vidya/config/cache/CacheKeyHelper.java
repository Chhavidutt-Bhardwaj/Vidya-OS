package com.ai.vidya.config;

import com.ai.vidya.tenant.TenantContext;

import java.util.UUID;

/**
 * Utility for building cache keys that are scoped to a tenant + school.
 *
 * <p>Every key embeds {@code tenantId:schoolId:...} to guarantee that
 * cached data from one tenant can never be served to another tenant —
 * even if the logical resource identifier is the same UUID.
 *
 * <h3>Usage in services</h3>
 * <pre>
 *   &#64;Cacheable(value = CacheConfig.STAFF_DETAIL, key = "T(com.ai.vidya.config.CacheKeyHelper).staffDetail(#id)")
 *   public StaffResponse getById(UUID id) { ... }
 * </pre>
 */
public final class CacheKeyHelper {

    private CacheKeyHelper() {}

    // ── Generic builder ───────────────────────────────────────────────────

    /**
     * Builds a key of the form {@code <tenantId>:<schoolId>:<suffix>}.
     * @param suffix any discriminating value (staff id, "list", etc.)
     */
    public static String key(String suffix) {
        UUID t = TenantContext.getTenantId();
        UUID s = TenantContext.getSchoolId();
        return (t != null ? t : "global") + ":" + (s != null ? s : "global") + ":" + suffix;
    }

    // ── Domain-specific helpers ───────────────────────────────────────────

    /** Key for a single staff entity. */
    public static String staffDetail(UUID staffId) {
        return key("staff:" + staffId);
    }

    /** Key for the paginated staff list (page + size + roleFilter baked in). */
    public static String staffList(int page, int size, String roleFilter) {
        return key("staff:list:" + page + ":" + size + ":" + roleFilter);
    }

    /** Key for a staff member's performance record. */
    public static String staffPerformance(UUID staffId) {
        return key("performance:" + staffId);
    }

    /** Key for AI insights for a staff member. */
    public static String aiInsights(UUID staffId) {
        return key("ai:insights:" + staffId);
    }

    /** Key for top-performers list. */
    public static String topPerformers(String roleType) {
        return key("ai:top:" + roleType);
    }

    /** Key for AI-generated recommendations. */
    public static String recommendations(String roleType) {
        return key("ai:recommendations:" + roleType);
    }
}
