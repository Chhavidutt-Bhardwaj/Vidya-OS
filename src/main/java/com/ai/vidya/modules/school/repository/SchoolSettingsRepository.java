package com.ai.vidya.modules.school.repository;

import com.ai.vidya.modules.school.entity.SchoolSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolSettingsRepository extends JpaRepository<SchoolSettings, UUID> {
    Optional<SchoolSettings> findBySchoolId(UUID schoolId);
}
