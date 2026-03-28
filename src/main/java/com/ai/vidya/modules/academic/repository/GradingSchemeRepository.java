package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.GradingScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradingSchemeRepository extends JpaRepository<GradingScheme, UUID> {

    @Query("SELECT g FROM GradingScheme g WHERE g.academicYear.id = :yearId AND g.deleted = false")
    List<GradingScheme> findByAcademicYearId(@Param("yearId") UUID yearId);

    @Query("SELECT g FROM GradingScheme g WHERE g.academicYear.id = :yearId AND g.defaultScheme = true AND g.deleted = false")
    Optional<GradingScheme> findDefaultByAcademicYearId(@Param("yearId") UUID yearId);
}