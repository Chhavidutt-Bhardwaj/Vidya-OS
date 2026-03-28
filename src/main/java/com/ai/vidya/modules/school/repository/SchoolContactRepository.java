package com.ai.vidya.modules.school.repository;

import com.ai.vidya.modules.school.entity.SchoolContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolContactRepository extends JpaRepository<SchoolContact, UUID> {
    List<SchoolContact> findBySchoolId(UUID schoolId);
    Optional<SchoolContact> findBySchoolIdAndPrimaryTrue(UUID schoolId);
}
