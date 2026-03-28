package com.ai.vidya.modules.student.repository;

import com.ai.vidya.modules.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    @Query("SELECT s FROM Student s WHERE s.schoolId = :schoolId AND s.status = 'ACTIVE' ORDER BY s.firstName, s.lastName")
    Page<Student> findActiveBySchoolId(@Param("schoolId") UUID schoolId, Pageable pageable);

    @Query("""
        SELECT s FROM Student s
        WHERE s.schoolId = :schoolId AND s.admissionNo = :admissionNo
        """)
    Optional<Student> findBySchoolIdAndAdmissionNo(
        @Param("schoolId")    UUID schoolId,
        @Param("admissionNo") String admissionNo);

    @Query("""
        SELECT s FROM Student s
        JOIN s.enrollments e
        WHERE s.schoolId      = :schoolId
          AND e.sectionId     = :sectionId
          AND e.academicYearId = :yearId
          AND e.status        = 'ACTIVE'
        ORDER BY e.rollNo
        """)
    List<Student> findBySectionAndYear(
        @Param("schoolId")  UUID schoolId,
        @Param("sectionId") UUID sectionId,
        @Param("yearId")    UUID yearId);

    @Query("""
        SELECT s FROM Student s
        LEFT JOIN FETCH s.guardians g
        LEFT JOIN FETCH s.enrollments e
        WHERE s.id = :id
        """)
    Optional<Student> findByIdWithDetails(@Param("id") UUID id);

    boolean existsBySchoolIdAndAdmissionNo(UUID schoolId, String admissionNo);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.schoolId = :schoolId AND s.status = 'ACTIVE'")
    long countActiveBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("""
        SELECT s FROM Student s
        WHERE s.schoolId = :schoolId
          AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR  LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
           OR  LOWER(s.admissionNo) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY s.firstName
        """)
    Page<Student> searchBySchoolId(
        @Param("schoolId") UUID schoolId,
        @Param("q")        String query,
        Pageable pageable);

    @Query(value = """
        SELECT s.* FROM students s
        JOIN student_enrollments e ON e.student_id = s.id AND e.academic_year_id = :yearId AND e.status = 'ACTIVE'
        JOIN (
            SELECT student_id,
                   ROUND(100.0 * SUM(CASE WHEN status = 'PRESENT' OR status = 'LATE' THEN 1 ELSE 0 END)
                         / NULLIF(COUNT(*), 0), 2) AS attendance_pct
            FROM student_attendance
            WHERE academic_year_id = :yearId AND period_number = 0
            GROUP BY student_id
        ) att ON att.student_id = s.id
        WHERE s.school_id = :schoolId AND att.attendance_pct < :threshold AND s.is_deleted = false
        LIMIT 100
        """, nativeQuery = true)
    List<Student> findAtRiskStudents(
        @Param("schoolId")  UUID schoolId,
        @Param("yearId")    UUID yearId,
        @Param("threshold") double threshold);
}
