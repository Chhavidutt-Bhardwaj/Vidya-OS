package com.ai.vidya.common.entity;

import com.ai.vidya.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * All school-scoped entities extend this.
 * school_id is auto-populated from TenantContext on @PrePersist.
 * This makes it impossible to accidentally save cross-tenant data.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAwareBaseEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @PrePersist
    protected void autoSetTenant() {
        if (this.schoolId == null) {
            UUID tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) {
                throw new IllegalStateException(
                    "TenantContext is empty. school_id cannot be null. " +
                    "Ensure JWT filter populated TenantContext for this request.");
            }
            this.schoolId = tenantId;
        }
    }
}