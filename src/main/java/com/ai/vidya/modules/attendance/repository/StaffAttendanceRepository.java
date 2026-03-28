package com.ai.vidya.modules.attendance.repository;

import com.ai.vidya.modules.attendance.entity.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, UUID> {

    Optional<StaffAttendance> findByStaffIdAndAttendanceDateAndDeletedFalse(
        UUID staffId, LocalDate date);

    @Query("""
        SELECT a FROM StaffAttendance a
        WHERE a.staffId = :staffId
          AND a.attendanceDate BETWEEN :from AND :to
          AND a.deleted = false
        ORDER BY a.attendanceDate
        """)
    List<StaffAttendance> findByStaffAndDateRange(
        @Param("staffId") UUID staffId,
        @Param("from")    LocalDate from,
        @Param("to")      LocalDate to);

    @Query("""
        SELECT a FROM StaffAttendance a
        WHERE a.schoolId = :schoolId AND a.attendanceDate = :date AND a.deleted = false
        ORDER BY a.staffId
        """)
    List<StaffAttendance> findBySchoolAndDate(
        @Param("schoolId") UUID schoolId,
        @Param("date")     LocalDate date);
}
