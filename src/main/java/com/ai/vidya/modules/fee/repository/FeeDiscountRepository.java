package com.ai.vidya.modules.fee.repository;

import com.ai.vidya.modules.fee.entity.FeeDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeDiscountRepository extends JpaRepository<FeeDiscount, UUID> {

    @Query("""
        SELECT d FROM FeeDiscount d
        WHERE d.studentId = :studentId AND d.academicYearId = :yearId AND d.active = true
          AND d.deleted = false
        """)
    List<FeeDiscount> findActiveByStudentAndYear(
        @Param("studentId") UUID studentId, @Param("yearId") UUID yearId);
}
