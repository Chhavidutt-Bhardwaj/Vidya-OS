package com.ai.vidya.common.enums;

/** How the late fee penalty is calculated when payment is overdue. */
public enum LateFeeType {

    /** Fixed rupee amount added after the grace period */
    FLAT,

    /** Percentage of the outstanding principal per month */
    PERCENTAGE_PER_MONTH,

    /** No late fee — grace period applies but no penalty */
    NONE
}