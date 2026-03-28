package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.school.entity.SchoolTerm;
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
public interface SchoolTermRepository extends JpaRepository<SchoolTerm, UUID> {

    @Query("""
        SELECT t FROM SchoolTerm t
        WHERE t.id = :id AND t.deleted = false
        """)
    Optional<SchoolTerm> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT t FROM SchoolTerm t
        WHERE t.academicYear.id = :yearId
          AND t.deleted = false
        ORDER BY t.sortOrder ASC
        """)
    List<SchoolTerm> findAllByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("""
        SELECT COUNT(t) > 0 FROM SchoolTerm t
        WHERE t.academicYear.id = :yearId
          AND t.name = :name
          AND t.deleted = false
        """)
    boolean existsByAcademicYearIdAndName(
        @Param("yearId") UUID yearId,
        @Param("name")   String name
    );

    @Query("""
        SELECT MAX(t.sortOrder) FROM SchoolTerm t
        WHERE t.academicYear.id = :yearId
          AND t.deleted = false
        """)
    Integer findMaxSortOrderByAcademicYearId(@Param("yearId") UUID yearId);

    @Modifying
    @Query("""
        UPDATE SchoolTerm t SET t.locked = true
        WHERE t.academicYear.id = :yearId
          AND t.endDate < CURRENT_DATE
          AND t.locked = false
          AND t.deleted = false
        """)
    int lockPastTermsForYear(@Param("yearId") UUID yearId);

    @Query("SELECT t FROM SchoolTerm t WHERE t.academicYear.id = :yearId AND t.deleted = false ORDER BY t.sortOrder")
    List<SchoolTerm> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Modifying
    @Query("UPDATE SchoolTerm t SET t.locked = true WHERE t.academicYear.id = :yearId")
    void lockAllByAcademicYearId(@Param("yearId") UUID yearId);
}
