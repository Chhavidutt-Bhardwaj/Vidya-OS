package com.ai.vidya.modules.staff.repository;

import com.ai.vidya.modules.staff.entity.StaffFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// StaffFeedbackRepository
// ─────────────────────────────────────────────────────────────────────────────

@Repository
interface StaffFeedbackRepository extends JpaRepository<StaffFeedback, UUID> {

    List<StaffFeedback> findAllByStaffIdAndTenantIdAndSchoolId(
            UUID staffId, UUID tenantId, UUID schoolId);

    @Query("""
            SELECT AVG(f.rating) FROM StaffFeedback f
            WHERE f.staffId   = :staffId
              AND f.tenantId  = :tenantId
              AND f.schoolId  = :schoolId
              AND f.academicYear = :year
            """)
    Optional<BigDecimal> avgRatingByStaffAndYear(
            @Param("staffId")  UUID staffId,
            @Param("tenantId") UUID tenantId,
            @Param("schoolId") UUID schoolId,
            @Param("year")     String year);
}

// ─────────────────────────────────────────────────────────────────────────────
// StaffPerformanceRepository
// ─────────────────────────────────────────────────────────────────────────────

@Repository
interface StaffPerformanceRepository extends JpaRepository<StaffPerformance, UUID> {

    List<StaffPerformance> findAllByStaffIdAndTenantIdAndSchoolId(
            UUID staffId, UUID tenantId, UUID schoolId);

    Optional<StaffPerformance> findByStaffIdAndTenantIdAndSchoolIdAndAcademicYear(
            UUID staffId, UUID tenantId, UUID schoolId, String academicYear);

    /** Attendance below threshold — used for alert generation. */
    @Query("""
            SELECT p FROM StaffPerformance p
            WHERE p.tenantId = :tenantId
              AND p.schoolId = :schoolId
              AND p.attendancePct < :threshold
              AND p.academicYear = :year
            """)
    List<StaffPerformance> findLowAttendance(
            @Param("tenantId")  UUID tenantId,
            @Param("schoolId")  UUID schoolId,
            @Param("threshold") BigDecimal threshold,
            @Param("year")      String year);
}
