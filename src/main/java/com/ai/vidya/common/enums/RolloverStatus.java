package com.ai.vidya.common.enums;

/**
 * Terminal state of a rollover attempt.
 * Written to RolloverAuditLog once the job completes.
 */
public enum RolloverStatus {

    /**
     * All configured clone steps completed without error.
     * The new AcademicYear is ready for admin review.
     */
    SUCCESS,

    /**
     * Some clone steps succeeded but at least one failed.
     * The new AcademicYear was created; admin must fix the partial data.
     * Details are in RolloverAuditLog.errorDetail.
     */
    PARTIAL,

    /**
     * The rollover failed before creating the new year,
     * or was rolled back entirely. Nothing was persisted.
     */
    FAILED,

    /**
     * Pre-flight check prevented rollover from starting.
     * Reasons: future year already exists, current year not locked,
     * autoRollover=false on the template, etc.
     */
    SKIPPED
}