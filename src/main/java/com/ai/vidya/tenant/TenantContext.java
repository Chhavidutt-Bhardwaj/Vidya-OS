package com.ai.vidya.tenant;

import java.util.UUID;

/**
 * ThreadLocal store for the current tenant (school).
 *
 * Populated by JwtAuthFilter on every authenticated request.
 * MUST be cleared in finally block — never let it leak across threads.
 *
 * For SUPER_ADMIN requests: schoolId = null (cross-tenant access).
 * For CHAIN_ADMIN requests: chainId is also stored for chain-wide queries.
 */
public final class TenantContext {

    private TenantContext() {}

    private static final ThreadLocal<UUID>   CURRENT_SCHOOL = new ThreadLocal<>();
    private static final ThreadLocal<UUID>   CURRENT_CHAIN  = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE   = new ThreadLocal<>();

    // ── Primary API (used by JwtAuthFilter) ───────────────────────────────
    public static void setCurrentTenant(UUID schoolId) { CURRENT_SCHOOL.set(schoolId); }
    public static UUID getCurrentTenant()              { return CURRENT_SCHOOL.get();  }
    public static void setCurrentChain(UUID chainId)   { CURRENT_CHAIN.set(chainId);  }
    public static UUID getCurrentChain()               { return CURRENT_CHAIN.get();  }
    public static void setCurrentRole(String role)     { CURRENT_ROLE.set(role);      }
    public static String getCurrentRole()              { return CURRENT_ROLE.get();   }

    // ── Aliases used by CacheKeyHelper & StaffService ─────────────────────
    public static void setTenantId(UUID tenantId)      { setCurrentTenant(tenantId); }
    public static UUID getTenantId()                   { return getCurrentTenant();  }
    public static void setSchoolId(UUID schoolId)      { setCurrentTenant(schoolId); }
    public static UUID getSchoolId()                   { return getCurrentTenant();  }

    // ── Guards used in service layer ──────────────────────────────────────
    public static UUID requireTenantId() {
        UUID id = getCurrentTenant();
        if (id == null) throw new IllegalStateException(
            "TenantContext has no tenantId — JWT filter may not have run for this request.");
        return id;
    }

    public static UUID requireSchoolId() {
        return requireTenantId(); // schoolId IS the tenantId in this system
    }

    /** Always call in JwtAuthFilter finally block */
    public static void clear() {
        CURRENT_SCHOOL.remove();
        CURRENT_CHAIN.remove();
        CURRENT_ROLE.remove();
    }

    public static boolean isSuperAdmin() { return "SUPER_ADMIN".equals(CURRENT_ROLE.get()); }
    public static boolean isChainAdmin() { return "CHAIN_ADMIN".equals(CURRENT_ROLE.get()); }
}
