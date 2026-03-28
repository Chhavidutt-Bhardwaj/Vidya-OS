package com.ai.vidya.modules.school.repository;

import com.ai.vidya.common.enums.OnboardingStep;
import com.ai.vidya.modules.school.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

    Optional<School> findByIdAndDeletedFalse(UUID id);

    boolean existsByUdiseCodeAndDeletedFalse(String udiseCode);

    List<School> findAllByChainIdAndDeletedFalse(UUID chainId);

    List<School> findAllByOnboardingStepAndDeletedFalse(OnboardingStep step);

    boolean existsByChainIdAndBranchCodeAndDeletedFalse(UUID chainId, String branchCode);

    @Query("""
        SELECT s FROM School s
        WHERE s.chain.id = :chainId
          AND s.headquarter = true
          AND s.deleted = false
        """)
    Optional<School> findHeadquarterByChainId(@Param("chainId") UUID chainId);

    /**
     * Lightweight name-only lookup used by AuthService to enrich the login
     * response without loading the full School entity.
     */
    @Query("SELECT s.name FROM School s WHERE s.id = :id AND s.deleted = false")
    Optional<String> findNameById(@Param("id") UUID id);

    /**
     * Paginated, filtered school list for GET /api/v1/schools
     *
     * WHY NATIVE SQL:
     * SchoolAddress extends @MappedSuperclass AddressEntity. JPQL resolves the
     * join alias to the concrete entity type (SchoolAddress), but the field
     * 'state' is declared on the non-entity superclass AddressEntity. Hibernate's
     * SQM path resolver throws UnknownPathException because it cannot navigate
     * mapped-superclass attributes through a JPQL join alias.
     *
     * Native SQL bypasses SQM entirely and queries the physical column directly.
     *
     * The countQuery is required when using native SQL with Pageable so Spring
     * Data can compute totalElements without fetching every row.
     *
     * Filter params: null  → match all rows (:param IS NULL short-circuits)
     *                value → exact match on state / type / plan column
     */
    @Query(
        value = """
            SELECT s.*
            FROM   schools s
            LEFT JOIN school_addresses a ON a.school_id = s.id
                                        AND a.is_deleted = false
            WHERE  s.is_deleted = false
              AND  (:state IS NULL OR a.state = :state)
              AND  (:type  IS NULL OR s.type  = :type)
              AND  (:plan  IS NULL OR s.plan  = :plan)
            """,
        countQuery = """
            SELECT COUNT(s.id)
            FROM   schools s
            LEFT JOIN school_addresses a ON a.school_id = s.id
                                        AND a.is_deleted = false
            WHERE  s.is_deleted = false
              AND  (:state IS NULL OR a.state = :state)
              AND  (:type  IS NULL OR s.type  = :type)
              AND  (:plan  IS NULL OR s.plan  = :plan)
            """,
        nativeQuery = true
    )
    Page<School> findAllByFilters(
        @Param("state") String state,
        @Param("type")  String type,
        @Param("plan")  String plan,
        Pageable pageable
    );

    /**
     * Paginated branch list for a chain — used by GET /chains/{chainId}/branches.
     */
    @Query("""
        SELECT s FROM School s
        WHERE s.chain.id = :chainId
          AND s.deleted = false
        """)
    Page<School> findAllByChainIdPaged(
        @Param("chainId") UUID chainId,
        Pageable pageable
    );
}