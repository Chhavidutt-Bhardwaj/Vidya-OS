package com.ai.vidya.common.enums;

/**
 * How often a fee head is charged within an academic year.
 *
 * The FeeStructureService uses this to generate individual
 * FeeInstalment rows for each student at the start of the year.
 */
public enum FeeFrequency {

    /** One lump sum for the entire academic year — e.g. registration, caution deposit */
    ANNUAL,

    /** Split into two instalments — typically per term */
    SEMI_ANNUAL,

    /** Four equal instalments per year */
    QUARTERLY,

    /** Twelve monthly instalments */
    MONTHLY,

    /**
     * Charged per event/trip — triggered ad-hoc.
     * Amount per head is stored; actual billing is event-driven.
     */
    ONE_TIME
}