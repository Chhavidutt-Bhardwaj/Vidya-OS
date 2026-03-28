package com.ai.vidya.common.enums;

/**
 * Classifies a holiday/event in the school calendar.
 *
 * Used by HolidayCalendar to drive UI rendering and
 * scheduling logic (e.g. working-day counters skip PUBLIC_HOLIDAY
 * and EXAM_BREAK rows automatically).
 */
public enum HolidayType {

    /** Central / state government gazette holiday — applies to all grades */
    PUBLIC_HOLIDAY,

    /** School-declared holiday (founder's day, annual day, sports day etc.) */
    SCHOOL_EVENT,

    /** Holiday declared during examination weeks */
    EXAM_BREAK,

    /**
     * Optional / restricted holiday — school is open but classes may be suspended.
     * e.g. local festival, half-day closures.
     */
    OPTIONAL_HOLIDAY,

    /** Parent-teacher meeting — no regular classes */
    PTM_DAY,

    /** School is closed for maintenance / infrastructure work */
    MAINTENANCE_BREAK
}