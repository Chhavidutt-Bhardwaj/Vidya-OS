package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.FeeStructureHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeStructureHeadRepository extends JpaRepository<FeeStructureHead, UUID> {

    @Query("SELECT h FROM FeeStructureHead h WHERE h.feeStructure.id = :structureId AND h.deleted = false ORDER BY h.sortOrder")
    List<FeeStructureHead> findByFeeStructureId(@Param("structureId") UUID structureId);

    @Query("SELECT h FROM FeeStructureHead h WHERE h.feeStructure.id = :structureId AND h.active = true AND h.deleted = false ORDER BY h.sortOrder")
    List<FeeStructureHead> findActiveByFeeStructureId(@Param("structureId") UUID structureId);
}