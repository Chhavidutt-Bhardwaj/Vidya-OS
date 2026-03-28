package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, UUID> {

    @Query("SELECT t FROM Timetable t WHERE t.academicYear.id = :yearId AND t.deleted = false")
    List<Timetable> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("""
        SELECT t FROM Timetable t
        WHERE t.academicYear.id = :yearId
          AND t.gradeRange.id   = :gradeRangeId
          AND t.active          = true
          AND t.deleted         = false
        """)
    Optional<Timetable> findActiveByYearAndGradeRange(
        @Param("yearId")       UUID yearId,
        @Param("gradeRangeId") UUID gradeRangeId
    );

    /** Deactivate old timetables before creating a revised one */
    @Modifying
    @Query("""
        UPDATE Timetable t SET t.active = false
        WHERE t.academicYear.id = :yearId
          AND t.gradeRange.id   = :gradeRangeId
          AND t.deleted         = false
        """)
    void deactivateByYearAndGradeRange(
        @Param("yearId")       UUID yearId,
        @Param("gradeRangeId") UUID gradeRangeId
    );
}