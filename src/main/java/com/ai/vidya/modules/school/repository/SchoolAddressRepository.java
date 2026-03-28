package com.ai.vidya.modules.school.repository;

import com.ai.vidya.modules.school.entity.SchoolAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolAddressRepository extends JpaRepository<SchoolAddress, UUID> {
    Optional<SchoolAddress> findBySchoolId(UUID schoolId);
}
