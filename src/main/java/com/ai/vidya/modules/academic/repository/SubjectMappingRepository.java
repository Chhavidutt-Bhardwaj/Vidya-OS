package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.SubjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectMappingRepository extends JpaRepository<SubjectMapping, UUID> {

    @Query("SELECT s FROM SubjectMapping s WHERE s.academicYear.id = :yearId AND s.deleted = false ORDER BY s.gradeRange.fromGradeOrder, s.sortOrder")
    List<SubjectMapping> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("SELECT s FROM SubjectMapping s WHERE s.academicYear.id = :yearId AND s.gradeRange.id = :gradeRangeId AND s.deleted = false ORDER BY s.sortOrder")
    List<SubjectMapping> findByAcademicYearIdAndGradeRangeId(
        @Param("yearId")       UUID yearId,
        @Param("gradeRangeId") UUID gradeRangeId
    );

    @Query("SELECT s FROM SubjectMapping s WHERE s.academicYear.id = :yearId AND s.gradeRange.id = :gradeRangeId AND s.subjectCode = :code AND s.deleted = false")
    Optional<SubjectMapping> findByYearGradeAndCode(
        @Param("yearId")       UUID yearId,
        @Param("gradeRangeId") UUID gradeRangeId,
        @Param("code")         String code
    );

    @Query("SELECT COUNT(s) > 0 FROM SubjectMapping s WHERE s.academicYear.id = :yearId AND s.gradeRange.id = :gradeRangeId AND s.subjectCode = :code AND s.deleted = false")
    boolean existsByYearGradeAndCode(
        @Param("yearId")       UUID yearId,
        @Param("gradeRangeId") UUID gradeRangeId,
        @Param("code")         String code
    );
}