package com.ai.vidya.modules.school.repository;

import com.ai.vidya.modules.school.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AcademicYear.
 */
@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    // ── Single lookups ─────────────────────────────────────────────────────

    @Query("""
        SELECT ay FROM AcademicYear ay
        WHERE ay.id = :id
          AND ay.deleted = false
        """)
    Optional<AcademicYear> findByIdNotDeleted(@Param("id") UUID id);

    /** Fetch with terms and shifts initialised — avoids N+1 in service layer. */
    @Query("""
        SELECT DISTINCT ay FROM AcademicYear ay
        LEFT JOIN FETCH ay.terms
        LEFT JOIN FETCH ay.shifts
        WHERE ay.id = :id
          AND ay.deleted = false
        """)
    Optional<AcademicYear> findByIdWithDetails(@Param("id") UUID id);

    // ── By school ──────────────────────────────────────────────────────────

    @Query("""
        SELECT ay FROM AcademicYear ay
        WHERE ay.school.id = :schoolId
          AND ay.deleted = false
        ORDER BY ay.startDate DESC
        """)
    List<AcademicYear> findAllBySchoolId(@Param("schoolId") UUID schoolId);

    /**
     * The current academic year for a school.
     * At most one row should have current = true per school
     * (enforced by the partial unique index uq_ay_school_current in V1 migration).
     */
    @Query("""
        SELECT ay FROM AcademicYear ay
        WHERE ay.school.id = :schoolId
          AND ay.current = true
          AND ay.deleted = false
        """)
    Optional<AcademicYear> findCurrentBySchoolId(@Param("schoolId") UUID schoolId);

    /**
     * Look up a specific academic year by its label within a school.
     * NOTE: the field is "label" on AcademicYear — NOT "yearLabel".
     * Derived method findBySchoolIdAndYearLabel would fail at startup;
     * this explicit query is the safe equivalent.
     */
    @Query("""
        SELECT ay FROM AcademicYear ay
        WHERE ay.school.id = :schoolId
          AND ay.label = :label
          AND ay.deleted = false
        """)
    Optional<AcademicYear> findBySchoolIdAndLabel(
        @Param("schoolId") UUID schoolId,
        @Param("label")    String label
    );

    /**
     * Uniqueness check before creating a new academic year for a school.
     * Equivalent of the broken: existsBySchoolIdAndYearLabel(...)
     */
    @Query("""
        SELECT COUNT(ay) > 0 FROM AcademicYear ay
        WHERE ay.school.id = :schoolId
          AND ay.label = :label
          AND ay.deleted = false
        """)
    boolean existsBySchoolIdAndLabel(
        @Param("schoolId") UUID schoolId,
        @Param("label")    String label
    );

    // ── Mutations ──────────────────────────────────────────────────────────

    /**
     * Clears the current flag on all years for a school before setting a new one.
     * Call this in a transaction before marking a new year as current.
     */
    @Modifying
    @Query("""
        UPDATE AcademicYear ay
        SET ay.current = false
        WHERE ay.school.id = :schoolId
          AND ay.deleted = false
        """)
    void clearCurrentFlagForSchool(@Param("schoolId") UUID schoolId);

    /**
     * Locks all academic years for a school that ended before today.
     * Typically run by a scheduled job at year-end.
     */
    @Modifying
    @Query("""
        UPDATE AcademicYear ay
        SET ay.locked = true
        WHERE ay.school.id = :schoolId
          AND ay.endDate < CURRENT_DATE
          AND ay.locked = false
          AND ay.deleted = false
        """)
    int lockPastYearsForSchool(@Param("schoolId") UUID schoolId);
}