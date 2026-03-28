package com.ai.vidya.common.enums;

/**
 * Represents the current onboarding step a school is at.
 * Steps must be completed in order.
 *
 * BASIC_INFO → CONTACT → ADDRESS → ACADEMIC → DOCUMENTS → COMPLETE
 */
public enum OnboardingStep {

    /** Step 1: School name, type, board, medium */
    BASIC_INFO,

    /** Step 2: Primary/secondary contacts, phone, email */
    CONTACT,

    /** Step 3: Physical address with coordinates */
    ADDRESS,

    /** Step 4: Academic year, grade ranges, shifts */
    ACADEMIC,

    /** Step 5: Affiliation docs, logo, certificates */
    DOCUMENTS,

    /** All steps done — school is fully onboarded */
    COMPLETE
}