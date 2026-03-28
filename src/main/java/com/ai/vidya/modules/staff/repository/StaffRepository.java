package com.ai.vidya.modules.staff.repository;

import com.ai.vidya.modules.staff.entity.Staff;
import com.ai.vidya.modules.staff.entity.StaffRoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Staff} and its subtypes.
 *
 * <p><b>Multi-tenancy rule:</b> Every query MUST filter by both
 * {@code tenantId} AND {@code schoolId}. Never omit either condition.
 *
 * <p>The {@code @SQLRestriction("is_deleted = false")} on the entity class
 * automatically adds the soft-delete filter to all generated SQL — no need
 * to add it manually to every query.
 */
@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    // ── Basic lookups ─────────────────────────────────────────────────────

    Optional<Staff> findByIdAndTenantIdAndSchoolId(UUID id, UUID tenantId, UUID schoolId);

    Optional<Staff> findByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    // ── Paginated list ────────────────────────────────────────────────────

    Page<Staff> findAllByTenantIdAndSchoolId(UUID tenantId, UUID schoolId, Pageable pageable);

    Page<Staff> findAllByTenantIdAndSchoolIdAndRoleType(
            UUID tenantId, UUID schoolId, StaffRoleType roleType, Pageable pageable);

    // ── Performance-based queries (for AI) ────────────────────────────────

    /**
     * Fetch top performers for a given role type, ordered by performance score DESC.
     * Works by joining to the teachers table; extend with UNION if other subtypes
     * gain a score column.
     */
    @Query("""
            SELECT t FROM Teacher t
            WHERE t.tenantId = :tenantId
              AND t.schoolId = :schoolId
              AND t.performanceScore IS NOT NULL
            ORDER BY t.performanceScore DESC
            """)
    List<Staff> findTopPerformingTeachers(
            @Param("tenantId") UUID tenantId,
            @Param("schoolId") UUID schoolId,
            Pageable pageable);

    /**
     * Fetch low performers (score below threshold) for training recommendations.
     */
    @Query("""
            SELECT t FROM Teacher t
            WHERE t.tenantId = :tenantId
              AND t.schoolId = :schoolId
              AND t.performanceScore < :threshold
            """)
    List<Staff> findLowPerformingTeachers(
            @Param("tenantId") UUID tenantId,
            @Param("schoolId") UUID schoolId,
            @Param("threshold") java.math.BigDecimal threshold);

    // ── Count helpers ─────────────────────────────────────────────────────

    long countByTenantIdAndSchoolId(UUID tenantId, UUID schoolId);

    long countByTenantIdAndSchoolIdAndRoleType(UUID tenantId, UUID schoolId, StaffRoleType roleType);
}
