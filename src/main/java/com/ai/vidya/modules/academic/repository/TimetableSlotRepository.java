package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.common.enums.SchoolDay;
import com.ai.vidya.modules.academic.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, UUID> {

    @Query("SELECT s FROM TimetableSlot s WHERE s.timetable.id = :timetableId AND s.deleted = false ORDER BY s.schoolDay, s.periodNumber")
    List<TimetableSlot> findByTimetableId(@Param("timetableId") UUID timetableId);

    @Query("SELECT s FROM TimetableSlot s WHERE s.timetable.id = :timetableId AND s.schoolDay = :day AND s.deleted = false ORDER BY s.periodNumber")
    List<TimetableSlot> findByTimetableIdAndDay(
        @Param("timetableId") UUID timetableId,
        @Param("day")         SchoolDay day
    );

    /**
     * Teacher clash detection — check if teacher is already assigned to another
     * slot at the same time on the same day across ALL timetables in this year.
     */
    @Query("""
        SELECT s FROM TimetableSlot s
        WHERE s.timetable.academicYear.id = :yearId
          AND s.teacherId                  = :teacherId
          AND s.schoolDay                  = :day
          AND s.deleted                    = false
          AND s.timetable.deleted          = false
          AND s.timetable.active           = true
          AND s.startTime                  < :endTime
          AND s.endTime                    > :startTime
          AND s.id                        != :excludeSlotId
        """)
    List<TimetableSlot> findTeacherClashes(
        @Param("yearId")        UUID yearId,
        @Param("teacherId")     UUID teacherId,
        @Param("day")           SchoolDay day,
        @Param("startTime")     LocalTime startTime,
        @Param("endTime")       LocalTime endTime,
        @Param("excludeSlotId") UUID excludeSlotId
    );

    /** Subject period count per week — used to validate periodsPerWeek target */
    @Query("""
        SELECT COUNT(s) FROM TimetableSlot s
        WHERE s.timetable.id  = :timetableId
          AND s.subjectCode   = :subjectCode
          AND s.deleted       = false
          AND s.freePeriod    = false
          AND s.breakSlot     = false
        """)
    long countWeeklyPeriodsForSubject(
        @Param("timetableId")  UUID timetableId,
        @Param("subjectCode")  String subjectCode
    );
}