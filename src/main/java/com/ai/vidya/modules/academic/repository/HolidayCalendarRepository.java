package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.common.enums.HolidayType;
import com.ai.vidya.modules.academic.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, UUID> {

    @Query("SELECT h FROM HolidayCalendar h WHERE h.academicYear.id = :yearId AND h.deleted = false ORDER BY h.holidayDate")
    List<HolidayCalendar> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("SELECT h FROM HolidayCalendar h WHERE h.academicYear.id = :yearId AND h.holidayType = :type AND h.deleted = false ORDER BY h.holidayDate")
    List<HolidayCalendar> findByAcademicYearIdAndType(
        @Param("yearId") UUID yearId,
        @Param("type") HolidayType type
    );

    /**
     * Count working days lost in a date window — used by attendance module.
     * Only counts holidays where affectsAttendance = true.
     */
    @Query("""
        SELECT COUNT(h) FROM HolidayCalendar h
        WHERE h.academicYear.id    = :yearId
          AND h.deleted            = false
          AND h.affectsAttendance  = true
          AND h.holidayDate       >= :from
          AND h.holidayDate       <= :to
        """)
    long countAffectingAttendance(
        @Param("yearId") UUID yearId,
        @Param("from")   LocalDate from,
        @Param("to")     LocalDate to
    );

    /** Cloneable events — excludes PUBLIC_HOLIDAY rows */
    @Query("""
        SELECT h FROM HolidayCalendar h
        WHERE h.academicYear.id = :yearId
          AND h.deleted         = false
          AND h.holidayType    != com.ai.vidya.common.enums.HolidayType.PUBLIC_HOLIDAY
        ORDER BY h.holidayDate
        """)
    List<HolidayCalendar> findCloneableByAcademicYearId(@Param("yearId") UUID yearId);
}