package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.common.enums.RolloverStatus;
import com.ai.vidya.modules.academic.entity.RolloverAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolloverAuditLogRepository extends JpaRepository<RolloverAuditLog, UUID> {

    @Query("SELECT r FROM RolloverAuditLog r WHERE r.school.id = :schoolId AND r.deleted = false ORDER BY r.performedAt DESC")
    Page<RolloverAuditLog> findBySchoolId(@Param("schoolId") UUID schoolId, Pageable pageable);

    @Query("SELECT r FROM RolloverAuditLog r WHERE r.school.id = :schoolId AND r.status = :status AND r.deleted = false ORDER BY r.performedAt DESC")
    Page<RolloverAuditLog> findBySchoolIdAndStatus(
        @Param("schoolId") UUID schoolId,
        @Param("status")   RolloverStatus status,
        Pageable pageable
    );

    /** Most recent successful rollover for a school — used for deduplication checks */
    @Query("""
        SELECT r FROM RolloverAuditLog r
        WHERE r.school.id = :schoolId
          AND r.status    = com.ai.vidya.common.enums.RolloverStatus.SUCCESS
          AND r.deleted   = false
        ORDER BY r.performedAt DESC
        LIMIT 1
        """)
    Optional<RolloverAuditLog> findLatestSuccessForSchool(@Param("schoolId") UUID schoolId);
}