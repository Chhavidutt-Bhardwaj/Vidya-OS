package com.ai.vidya.modules.exam.repository;

import com.ai.vidya.modules.exam.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, UUID> {

    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.studentId = :studentId AND r.academicYearId = :yearId AND r.deleted = false
        ORDER BY r.subjectCode
        """)
    List<ExamResult> findByStudentAndYear(
        @Param("studentId") UUID studentId, @Param("yearId") UUID yearId);

    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.examScheduleId = :examId AND r.sectionId = :sectionId AND r.deleted = false
        ORDER BY r.studentId, r.subjectCode
        """)
    List<ExamResult> findByExamAndSection(
        @Param("examId") UUID examId, @Param("sectionId") UUID sectionId);

    @Query("""
        SELECT r FROM ExamResult r
        WHERE r.studentId = :studentId AND r.academicYearId = :yearId
          AND r.examScheduleId IN (
              SELECT es.id FROM ExamSchedule es WHERE es.term.id = :termId AND es.deleted = false
          )
          AND r.deleted = false
        ORDER BY r.subjectCode
        """)
    List<ExamResult> findByStudentAndYearAndTerm(
        @Param("studentId") UUID studentId,
        @Param("yearId")    UUID yearId,
        @Param("termId")    UUID termId);

    @Modifying
    @Query("""
        UPDATE ExamResult r SET r.published = true, r.publishedAt = :now, r.publishedBy = :by
        WHERE r.examScheduleId = :examId AND r.deleted = false
        """)
    int publishByExamSchedule(
        @Param("examId") UUID examId, @Param("now") LocalDateTime now, @Param("by") UUID by);

    @Query(value = """
        SELECT r.student_id AS studentId, AVG(r.percentage) AS averagePercentage
        FROM exam_results r
        WHERE r.school_id = :schoolId AND r.academic_year_id = :yearId
          AND r.is_deleted = false AND r.is_published = true AND r.percentage IS NOT NULL
        GROUP BY r.student_id
        HAVING AVG(r.percentage) < :threshold
        LIMIT 100
        """, nativeQuery = true)
    List<java.util.Map<String, Object>> findLowPerformingStudents(
        @Param("schoolId")  UUID schoolId,
        @Param("yearId")    UUID yearId,
        @Param("threshold") BigDecimal threshold);
}
