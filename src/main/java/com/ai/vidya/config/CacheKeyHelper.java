package com.ai.vidya.config;

import com.ai.vidya.tenant.TenantContext;

import java.util.UUID;

/**
 * Tenant-scoped cache key builder.
 * Every key embeds tenantId:schoolId to prevent cross-tenant cache pollution.
 */
public final class CacheKeyHelper {

    private CacheKeyHelper() {}

    public static String key(String suffix) {
        UUID t = TenantContext.getTenantId();
        UUID s = TenantContext.getSchoolId();
        return (t != null ? t : "global") + ":" + (s != null ? s : "global") + ":" + suffix;
    }

    public static String staffDetail(UUID staffId)                      { return key("staff:" + staffId); }
    public static String staffList(int page, int size, String roleFilter){ return key("staff:list:" + page + ":" + size + ":" + roleFilter); }
    public static String staffPerformance(UUID staffId)                  { return key("performance:" + staffId); }
    public static String aiInsights(UUID staffId)                        { return key("ai:insights:" + staffId); }
    public static String topPerformers(String roleType)                  { return key("ai:top:" + roleType); }
    public static String recommendations(String roleType)                { return key("ai:recommendations:" + roleType); }
}
