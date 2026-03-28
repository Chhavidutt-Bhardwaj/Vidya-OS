package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.school.entity.SchoolShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** All queries explicit — no derived method names. */
@Repository
public interface SchoolShiftRepository extends JpaRepository<SchoolShift, UUID> {

    @Query("""
        SELECT s FROM SchoolShift s
        WHERE s.id = :id AND s.deleted = false
        """)
    Optional<SchoolShift> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT s FROM SchoolShift s
        WHERE s.academicYear.id = :yearId
          AND s.deleted = false
        ORDER BY s.startTime ASC
        """)
    List<SchoolShift> findAllByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("""
        SELECT s FROM SchoolShift s
        WHERE s.academicYear.id = :yearId
          AND s.defaultShift = true
          AND s.deleted = false
        """)
    Optional<SchoolShift> findDefaultByAcademicYearId(@Param("yearId") UUID yearId);

    @Modifying
    @Query("""
        UPDATE SchoolShift s SET s.defaultShift = false
        WHERE s.academicYear.id = :yearId
          AND s.deleted = false
        """)
    void clearDefaultFlagForYear(@Param("yearId") UUID yearId);

    @Query("""
        SELECT COUNT(s) > 0 FROM SchoolShift s
        WHERE s.academicYear.id = :yearId
          AND s.name = :name
          AND s.deleted = false
        """)
    boolean existsByAcademicYearIdAndName(
        @Param("yearId") UUID yearId,
        @Param("name")   String name
    );

    @Query("SELECT s FROM SchoolShift s WHERE s.academicYear.id = :yearId AND s.deleted = false ORDER BY s.startTime")
    List<SchoolShift> findByAcademicYearId(@Param("yearId") UUID yearId);

}
