package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.common.enums.ExamType;
import com.ai.vidya.modules.academic.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, UUID> {

    @Query("SELECT e FROM ExamSchedule e WHERE e.term.id = :termId AND e.deleted = false ORDER BY e.fromDate")
    List<ExamSchedule> findByTermId(@Param("termId") UUID termId);

    @Query("""
        SELECT e FROM ExamSchedule e
        WHERE e.term.academicYear.id = :yearId
          AND e.deleted              = false
        ORDER BY e.fromDate
        """)
    List<ExamSchedule> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("""
        SELECT e FROM ExamSchedule e
        WHERE e.term.academicYear.id = :yearId
          AND e.examType             = :type
          AND e.deleted              = false
        ORDER BY e.fromDate
        """)
    List<ExamSchedule> findByAcademicYearIdAndType(
        @Param("yearId") UUID yearId,
        @Param("type")   ExamType type
    );
}