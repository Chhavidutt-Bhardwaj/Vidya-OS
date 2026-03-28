package com.ai.vidya.common.enums;

/**
 * Records what initiated a year rollover.
 * Stored on RolloverAuditLog for support and debugging.
 */
public enum RolloverTrigger {

    /** Automated rollover fired by the @Scheduled job */
    SCHEDULER,

    /** Manually triggered by a SUPER_ADMIN or SCHOOL_ADMIN via the admin UI */
    ADMIN_MANUAL,

    /** Triggered via REST API (e.g. from a migration script or integration) */
    API
}