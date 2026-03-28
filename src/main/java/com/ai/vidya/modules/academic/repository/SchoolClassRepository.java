package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    @Query("SELECT c FROM SchoolClass c WHERE c.id = :id AND c.deleted = false")
    Optional<SchoolClass> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT c FROM SchoolClass c
        WHERE c.academicYear.id = :yearId
          AND c.deleted = false
        ORDER BY c.gradeOrder ASC
        """)
    List<SchoolClass> findAllByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("""
        SELECT c FROM SchoolClass c
        WHERE c.school.id = :schoolId
          AND c.academicYear.id = :yearId
          AND c.deleted = false
        ORDER BY c.gradeOrder ASC
        """)
    List<SchoolClass> findAllBySchoolIdAndYearId(
        @Param("schoolId") UUID schoolId,
        @Param("yearId")   UUID yearId
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM SchoolClass c
        WHERE c.academicYear.id = :yearId
          AND c.name = :name
          AND c.deleted = false
        """)
    boolean existsByYearIdAndName(
        @Param("yearId") UUID yearId,
        @Param("name")   String name
    );

    /** Fetch with sections and subjects eagerly — avoids N+1 in detail view. */
    @Query("""
        SELECT DISTINCT c FROM SchoolClass c
        LEFT JOIN FETCH c.sections s
        LEFT JOIN FETCH c.classSubjects cs
        LEFT JOIN FETCH cs.subject
        WHERE c.id = :id AND c.deleted = false
        """)
    Optional<SchoolClass> findByIdWithDetails(@Param("id") UUID id);
}
