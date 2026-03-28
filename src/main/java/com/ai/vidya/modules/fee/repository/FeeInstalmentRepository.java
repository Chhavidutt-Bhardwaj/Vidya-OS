package com.ai.vidya.modules.fee.repository;

import com.ai.vidya.modules.fee.dto.response.FeeDefaulterResponse;
import com.ai.vidya.modules.fee.entity.FeeInstalment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeeInstalmentRepository extends JpaRepository<FeeInstalment, UUID> {

    @Query("""
        SELECT i FROM FeeInstalment i
        WHERE i.studentId = :studentId AND i.academicYearId = :yearId AND i.deleted = false
        ORDER BY i.dueDate, i.instalmentNumber
        """)
    List<FeeInstalment> findByStudentAndYear(
        @Param("studentId") UUID studentId, @Param("yearId") UUID yearId);

    @Query("""
        SELECT COALESCE(SUM(i.amountPaid), 0) FROM FeeInstalment i
        WHERE i.schoolId = :schoolId AND i.academicYearId = :yearId AND i.deleted = false
        """)
    BigDecimal sumCollectedBySchoolAndYear(
        @Param("schoolId") UUID schoolId, @Param("yearId") UUID yearId);

    @Query("""
        SELECT COALESCE(SUM(i.netAmount - i.amountPaid), 0) FROM FeeInstalment i
        WHERE i.schoolId = :schoolId AND i.academicYearId = :yearId
          AND i.status NOT IN ('PAID','WAIVED','CANCELLED') AND i.deleted = false
        """)
    BigDecimal sumPendingBySchoolAndYear(
        @Param("schoolId") UUID schoolId, @Param("yearId") UUID yearId);

    @Query(value = """
        SELECT
            fi.student_id AS studentId,
            s.first_name || ' ' || s.last_name AS studentName,
            s.admission_no AS admissionNo,
            SUM(fi.net_amount - fi.amount_paid) AS overdueAmount,
            MIN(fi.due_date) AS oldestDueDate,
            COUNT(*) AS overdueCount
        FROM fee_instalments fi
        JOIN students s ON s.id = fi.student_id
        WHERE fi.school_id = :schoolId
          AND fi.academic_year_id = :yearId
          AND fi.status IN ('PENDING','PARTIAL','OVERDUE')
          AND fi.due_date < :cutoffDate
          AND fi.is_deleted = false
          AND s.is_deleted = false
        GROUP BY fi.student_id, s.first_name, s.last_name, s.admission_no
        HAVING SUM(fi.net_amount - fi.amount_paid) > 0
        ORDER BY overdueAmount DESC
        LIMIT 200
        """, nativeQuery = true)
    List<FeeDefaulterResponse> findDefaulters(
        @Param("schoolId")    UUID schoolId,
        @Param("yearId")      UUID yearId,
        @Param("cutoffDate")  LocalDate cutoffDate);

    @Query(value = """
        SELECT fi.student_id AS studentId, SUM(fi.net_amount - fi.amount_paid) AS overdueAmount
        FROM fee_instalments fi
        WHERE fi.school_id = :schoolId AND fi.academic_year_id = :yearId
          AND fi.status IN ('PENDING','PARTIAL','OVERDUE')
          AND CURRENT_DATE - fi.due_date > :overdueDays
          AND fi.is_deleted = false
        GROUP BY fi.student_id
        HAVING SUM(fi.net_amount - fi.amount_paid) > 100
        """, nativeQuery = true)
    List<java.util.Map<String, Object>> findHighValueDefaulters(
        @Param("schoolId")   UUID schoolId,
        @Param("yearId")     UUID yearId,
        @Param("overdueDays") int overdueDays);
}
