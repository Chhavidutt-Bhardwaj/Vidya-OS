package com.ai.vidya.modules.staff.service;

import com.ai.vidya.config.CacheConfig;
import com.ai.vidya.config.CacheKeyHelper;
import com.ai.vidya.modules.staff.dto.request.FeedbackRequest;
import com.ai.vidya.modules.staff.dto.response.PerformanceResponse;
import com.ai.vidya.modules.staff.entity.StaffFeedback;
import com.ai.vidya.modules.staff.entity.StaffPerformance;
import com.ai.vidya.modules.staff.entity.Teacher;
import com.ai.vidya.modules.staff.repository.StaffFeedbackRepository;
import com.ai.vidya.modules.staff.repository.StaffPerformanceRepository;
import com.ai.vidya.modules.staff.repository.StaffRepository;
import com.ai.vidya.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Manages staff feedback submission and performance snapshot computation.
 *
 * <h3>Performance formula (teacher)</h3>
 * <pre>
 *   overallScore = (attendancePct × 0.30) + (resultPct × 0.40) + (avgRating × 20 × 0.30)
 * </pre>
 * Weights are configurable via application.yml when externalised.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PerformanceService {

    private static final BigDecimal ATTENDANCE_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal RESULT_WEIGHT     = new BigDecimal("0.40");
    private static final BigDecimal FEEDBACK_WEIGHT   = new BigDecimal("0.30");
    private static final BigDecimal FEEDBACK_SCALE    = new BigDecimal("20"); // 1–5 → 0–100

    private final StaffRepository            staffRepository;
    private final StaffFeedbackRepository    feedbackRepository;
    private final StaffPerformanceRepository performanceRepository;

    // ── Submit feedback ───────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = CacheConfig.STAFF_PERFORMANCE,
                key = "T(com.ai.vidya.config.CacheKeyHelper).staffPerformance(#staffId)")
    public void submitFeedback(UUID staffId, FeedbackRequest req) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        // Validate staff exists in this tenant+school
        staffRepository.findByIdAndTenantIdAndSchoolId(staffId, tenantId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + staffId));

        StaffFeedback feedback = StaffFeedback.builder()
                .staffId(staffId)
                .feedbackSource(req.getFeedbackSource())
                .rating(req.getRating())
                .comments(req.getComments())
                .academicYear(req.getAcademicYear())
                .feedbackDate(req.getFeedbackDate())
                .build();
        feedback.setTenantId(tenantId);
        feedback.setSchoolId(schoolId);

        feedbackRepository.save(feedback);
        log.info("Feedback submitted for staffId={} year={}", staffId, req.getAcademicYear());

        // Recompute and persist the performance snapshot immediately
        recomputePerformance(staffId, req.getAcademicYear());
    }

    // ── Get performance ───────────────────────────────────────────────────

    @Cacheable(
        value  = CacheConfig.STAFF_PERFORMANCE,
        key    = "T(com.ai.vidya.config.CacheKeyHelper).staffPerformance(#staffId)",
        unless = "#result == null"
    )
    public PerformanceResponse getPerformance(UUID staffId, String academicYear) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        StaffPerformance perf = performanceRepository
                .findByStaffIdAndTenantIdAndSchoolIdAndAcademicYear(staffId, tenantId, schoolId, academicYear)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No performance record for staffId=" + staffId + " year=" + academicYear));

        return toDto(perf);
    }

    // ── Internal: recompute + persist ─────────────────────────────────────

    @Transactional
    void recomputePerformance(UUID staffId, String academicYear) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        // Current average feedback rating for the year
        BigDecimal avgRating = feedbackRepository
                .avgRatingByStaffAndYear(staffId, tenantId, schoolId, academicYear)
                .orElse(BigDecimal.ZERO);

        // Load existing snapshot or create new
        StaffPerformance perf = performanceRepository
                .findByStaffIdAndTenantIdAndSchoolIdAndAcademicYear(staffId, tenantId, schoolId, academicYear)
                .orElseGet(() -> {
                    StaffPerformance p = StaffPerformance.builder()
                            .staffId(staffId)
                            .academicYear(academicYear)
                            .periodStart(LocalDate.of(Integer.parseInt(academicYear.split("-")[0]), 4, 1))
                            .periodEnd(LocalDate.of(Integer.parseInt(academicYear.split("-")[0]) + 1, 3, 31))
                            .build();
                    p.setTenantId(tenantId);
                    p.setSchoolId(schoolId);
                    return p;
                });

        // Populate feedback component; attendancePct and resultPct come from other modules
        perf.setAvgFeedbackRating(avgRating.setScale(2, RoundingMode.HALF_UP));

        // Compute overall score (use defaults for missing attendance/result data)
        BigDecimal attendance = perf.getAttendancePct() != null ? perf.getAttendancePct() : BigDecimal.ZERO;
        BigDecimal result     = perf.getResultPct()     != null ? perf.getResultPct()     : BigDecimal.ZERO;
        BigDecimal feedback100 = avgRating.multiply(FEEDBACK_SCALE); // 1–5 → 0–100

        BigDecimal overall = attendance.multiply(ATTENDANCE_WEIGHT)
                .add(result.multiply(RESULT_WEIGHT))
                .add(feedback100.multiply(FEEDBACK_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);

        perf.setOverallScore(overall);
        performanceRepository.save(perf);

        // If this staff is a Teacher, update their denormalized performanceScore
        staffRepository.findByIdAndTenantIdAndSchoolId(staffId, tenantId, schoolId)
                .filter(s -> s instanceof Teacher)
                .map(s -> (Teacher) s)
                .ifPresent(t -> {
                    t.setPerformanceScore(overall);
                    staffRepository.save(t);
                });
    }

    // ── DTO mapping ───────────────────────────────────────────────────────

    private PerformanceResponse toDto(StaffPerformance p) {
        return PerformanceResponse.builder()
                .staffId(p.getStaffId())
                .academicYear(p.getAcademicYear())
                .attendancePct(p.getAttendancePct())
                .resultPct(p.getResultPct())
                .avgFeedbackRating(p.getAvgFeedbackRating())
                .overallScore(p.getOverallScore())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .build();
    }
}
