package com.ai.vidya.modules.fee.repository;

import com.ai.vidya.modules.fee.entity.FeePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, UUID> {

    Optional<FeePayment> findBySchoolIdAndReceiptNoAndDeletedFalse(UUID schoolId, String receiptNo);

    @Query("""
        SELECT p FROM FeePayment p WHERE p.studentId = :studentId AND p.academicYearId = :yearId
          AND p.deleted = false ORDER BY p.paymentDate DESC
        """)
    List<FeePayment> findByStudentAndYear(
        @Param("studentId") UUID studentId, @Param("yearId") UUID yearId);

    Page<FeePayment> findBySchoolIdAndPaymentDateBetweenAndDeletedFalse(
        UUID schoolId, LocalDate from, LocalDate to, Pageable pageable);
}
