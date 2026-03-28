package com.ai.vidya.modules.attendance.repository;

import com.ai.vidya.modules.attendance.entity.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {

    @Query("""
        SELECT a FROM StudentAttendance a
        WHERE a.sectionId = :sectionId AND a.attendanceDate = :date AND a.periodNumber = :period
          AND a.deleted = false
        ORDER BY a.studentId
        """)
    List<StudentAttendance> findBySectionAndDate(
        @Param("sectionId") UUID sectionId,
        @Param("date")      LocalDate date,
        @Param("period")    int period);

    @Query("""
        SELECT a FROM StudentAttendance a
        WHERE a.studentId = :studentId AND a.academicYearId = :yearId
          AND a.attendanceDate BETWEEN :from AND :to
          AND a.deleted = false
        ORDER BY a.attendanceDate, a.periodNumber
        """)
    List<StudentAttendance> findByStudentAndDateRange(
        @Param("studentId") UUID studentId,
        @Param("yearId")    UUID yearId,
        @Param("from")      LocalDate from,
        @Param("to")        LocalDate to);

    @Query("""
        SELECT COUNT(a) > 0 FROM StudentAttendance a
        WHERE a.sectionId = :sectionId AND a.attendanceDate = :date
          AND a.periodNumber = :period AND a.deleted = false
        """)
    boolean existsBySectionAndDateAndPeriod(
        @Param("sectionId") UUID sectionId,
        @Param("date")      LocalDate date,
        @Param("period")    int period);

    @Modifying
    @Query("""
        UPDATE StudentAttendance a SET a.deleted = true, a.deletedAt = CURRENT_TIMESTAMP
        WHERE a.sectionId = :sectionId AND a.attendanceDate = :date AND a.periodNumber = :period
        """)
    void deleteBySectionAndDateAndPeriod(
        @Param("sectionId") UUID sectionId,
        @Param("date")      LocalDate date,
        @Param("period")    int period);

    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE period_number = 0)  AS total_days,
            COUNT(*) FILTER (WHERE period_number = 0 AND status IN ('PRESENT','LATE')) AS present_days,
            ROUND(
                100.0 * COUNT(*) FILTER (WHERE period_number = 0 AND status IN ('PRESENT','LATE'))
                / NULLIF(COUNT(*) FILTER (WHERE period_number = 0), 0),
                2
            ) AS attendance_pct
        FROM student_attendance
        WHERE student_id = :studentId AND academic_year_id = :yearId
          AND attendance_date BETWEEN :from AND :to AND is_deleted = false
        """, nativeQuery = true)
    java.util.Map<String, Object> getSummaryStats(
        @Param("studentId") UUID studentId,
        @Param("yearId")    UUID yearId,
        @Param("from")      LocalDate from,
        @Param("to")        LocalDate to);

    @Query("""
        SELECT a FROM StudentAttendance a
        WHERE a.schoolId = :schoolId AND a.academicYearId = :yearId
          AND a.attendanceDate = :date AND a.periodNumber = 0 AND a.status = 'ABSENT'
          AND a.deleted = false
        """)
    List<StudentAttendance> findAbsentStudentsOnDate(
        @Param("schoolId") UUID schoolId,
        @Param("yearId")   UUID yearId,
        @Param("date")     LocalDate date);
}
