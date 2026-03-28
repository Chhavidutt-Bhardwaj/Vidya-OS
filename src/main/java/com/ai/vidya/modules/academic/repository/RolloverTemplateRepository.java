package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.RolloverTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolloverTemplateRepository extends JpaRepository<RolloverTemplate, UUID> {

    @Query("SELECT r FROM RolloverTemplate r WHERE r.school.id = :schoolId AND r.deleted = false")
    Optional<RolloverTemplate> findBySchoolId(@Param("schoolId") UUID schoolId);
}