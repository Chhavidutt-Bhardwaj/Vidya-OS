package com.ai.vidya.common.enums;

/**
 * Working days used in timetable slot scheduling.
 *<p>
 * We define our own enum (rather than java.time.DayOfWeek)
 * so Saturday/Sunday visibility can be controlled per-school
 * via SchoolSettings.saturdayWorking without introducing
 * null-safety issues or flag checks across the timetable layer.
 */
public enum SchoolDay {

    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,

    /**
     * Included when SchoolSettings.saturdayWorking = true.
     * TimetableService skips SATURDAY slots for schools where
     * saturdayWorking = false or alternateSaturdayOff = true.
     */
    SATURDAY
}