package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.school.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    // ── Current year ───────────────────────────────────────────────────────

    @Query("SELECT ay FROM AcademicYear ay WHERE ay.school.id = :schoolId AND ay.current = true AND ay.deleted = false")
    Optional<AcademicYear> findCurrentBySchoolId(@Param("schoolId") UUID schoolId);

    /** Bulk-clear isCurrent on all years for a school — called before setting a new current. */
    @Modifying
    @Query("UPDATE AcademicYear ay SET ay.current = false WHERE ay.school.id = :schoolId AND ay.deleted = false")
    void clearCurrentForSchool(@Param("schoolId") UUID schoolId);

    // ── Existence checks ───────────────────────────────────────────────────

    @Query("SELECT COUNT(ay) > 0 FROM AcademicYear ay WHERE ay.school.id = :schoolId AND ay.label = :label AND ay.deleted = false")
    boolean existsBySchoolIdAndLabel(@Param("schoolId") UUID schoolId, @Param("label") String label);

    /**
     * Check whether the school already has an academic year whose date range
     * overlaps with the proposed [startDate, endDate] window.
     * Used to prevent gap-free year stacking and overlap conflicts.
     */
    @Query("""
        SELECT COUNT(ay) > 0
        FROM AcademicYear ay
        WHERE ay.school.id = :schoolId
          AND ay.deleted   = false
          AND ay.id       != :excludeId
          AND ay.startDate < :endDate
          AND ay.endDate   > :startDate
        """)
    boolean hasOverlappingYear(
        @Param("schoolId")  UUID schoolId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate,
        @Param("excludeId") UUID excludeId
    );

    // ── Listing ────────────────────────────────────────────────────────────

    @Query("SELECT ay FROM AcademicYear ay WHERE ay.school.id = :schoolId AND ay.deleted = false ORDER BY ay.startDate DESC")
    List<AcademicYear> findAllBySchoolId(@Param("schoolId") UUID schoolId);

    // ── Rollover eligibility ───────────────────────────────────────────────

    /**
     * Find all schools whose CURRENT academic year ends on or before
     * {@code cutoffDate} and that do NOT yet have a year starting after
     * the current year's end date.
     *
     * Called nightly by YearRolloverJob to find schools due for auto-rollover.
     */
    @Query("""
        SELECT ay FROM AcademicYear ay
        WHERE ay.current    = true
          AND ay.deleted    = false
          AND ay.endDate   <= :cutoffDate
          AND NOT EXISTS (
              SELECT 1 FROM AcademicYear future
              WHERE future.school.id  = ay.school.id
                AND future.deleted   = false
                AND future.startDate > ay.endDate
          )
        """)
    List<AcademicYear> findYearsDueForRollover(@Param("cutoffDate") LocalDate cutoffDate);

    // ── Locking ────────────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE AcademicYear ay SET ay.locked = true WHERE ay.id = :id")
    void lockYear(@Param("id") UUID id);

    @Query("SELECT ay.locked FROM AcademicYear ay WHERE ay.id = :id")
    boolean isLocked(@Param("id") UUID id);
}