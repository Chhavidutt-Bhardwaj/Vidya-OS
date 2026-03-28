package com.ai.vidya.modules.student.repository;

import com.ai.vidya.modules.student.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    @Query("""
        SELECT e FROM StudentEnrollment e
        WHERE e.student.id = :studentId
          AND e.academicYearId = :yearId
          AND e.status = 'ACTIVE'
          AND e.deleted = false
        """)
    Optional<StudentEnrollment> findActiveByStudentAndYear(
        @Param("studentId") UUID studentId,
        @Param("yearId")    UUID yearId);

    @Query("""
        SELECT e FROM StudentEnrollment e
        WHERE e.sectionId = :sectionId AND e.academicYearId = :yearId AND e.status = 'ACTIVE' AND e.deleted = false
        ORDER BY e.rollNo
        """)
    List<StudentEnrollment> findActiveBySectionAndYear(
        @Param("sectionId") UUID sectionId,
        @Param("yearId")    UUID yearId);

    @Query("SELECT COUNT(e) FROM StudentEnrollment e WHERE e.sectionId = :sectionId AND e.academicYearId = :yearId AND e.status = 'ACTIVE' AND e.deleted = false")
    long countActiveBySectionAndYear(@Param("sectionId") UUID sectionId, @Param("yearId") UUID yearId);
}
