package com.ai.vidya.modules.school.repository;

import com.ai.vidya.modules.school.entity.SchoolOnboardingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SchoolOnboardingAuditRepository extends JpaRepository<SchoolOnboardingAudit, UUID> {

    List<SchoolOnboardingAudit> findAllBySchoolIdOrderByCreatedAtDesc(UUID schoolId);
}