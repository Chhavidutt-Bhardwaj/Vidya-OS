package com.ai.vidya.common.enums;

/**
 * Classifies a fee head in the school fee structure.
 *
 * Rolled over automatically each year; amounts may be updated
 * before the new year is activated.
 */
public enum FeeHeadType {

    /** Core tuition / academic fee */
    TUITION,

    /** School bus / van transportation */
    TRANSPORT,

    /** Library membership / book deposit */
    LIBRARY,

    /** Sports / games / PE activities */
    SPORTS,

    /** Computer lab / IT infrastructure */
    COMPUTER_LAB,

    /** Science / chemistry lab consumables */
    SCIENCE_LAB,

    /** Examination fee — internal exams */
    EXAMINATION,

    /** Annual day / cultural events */
    ANNUAL_FUNCTION,

    /** Smart class / e-learning infrastructure */
    SMART_CLASS,

    /** Hostel / boarding fee */
    HOSTEL,

    /** Mess / canteen */
    MESS,

    /** Medical / health facility */
    MEDICAL,

    /** Uniform fee (collected centrally) */
    UNIFORM,

    /** Books / stationery provided by school */
    BOOKS_STATIONERY,

    /** One-time refundable security / caution deposit */
    SECURITY_DEPOSIT,

    /** Registration / admission fee (one-time) */
    ADMISSION,

    /** Re-examination / re-admission */
    RE_EXAMINATION,

    /** Miscellaneous — any school-specific head not in this list */
    MISCELLANEOUS
}