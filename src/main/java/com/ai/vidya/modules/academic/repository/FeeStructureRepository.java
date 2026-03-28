package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    @Query("SELECT f FROM FeeStructure f WHERE f.academicYear.id = :yearId AND f.deleted = false ORDER BY f.name")
    List<FeeStructure> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("SELECT f FROM FeeStructure f WHERE f.academicYear.id = :yearId AND f.active = true AND f.deleted = false")
    List<FeeStructure> findActiveByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("SELECT f FROM FeeStructure f WHERE f.academicYear.id = :yearId AND f.gradeRange.id = :gradeRangeId AND f.deleted = false")
    Optional<FeeStructure> findByAcademicYearIdAndGradeRangeId(
        @Param("yearId")       UUID yearId,
        @Param("gradeRangeId") UUID gradeRangeId
    );

    /** Activate all structures for a year — called when AcademicYear is activated. */
    @Modifying
    @Query("UPDATE FeeStructure f SET f.active = true WHERE f.academicYear.id = :yearId AND f.deleted = false")
    void activateAllByAcademicYearId(@Param("yearId") UUID yearId);

    /** Deactivate all structures for a year — called when a different year is activated. */
    @Modifying
    @Query("UPDATE FeeStructure f SET f.active = false WHERE f.academicYear.id = :yearId AND f.deleted = false")
    void deactivateAllByAcademicYearId(@Param("yearId") UUID yearId);
}