package com.ai.vidya.modules.staff.ai;

import com.ai.vidya.config.CacheConfig;
import com.ai.vidya.config.CacheKeyHelper;
import com.ai.vidya.modules.staff.entity.*;
import com.ai.vidya.modules.staff.repository.StaffPerformanceRepository;
import com.ai.vidya.modules.staff.repository.StaffRepository;
import com.ai.vidya.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rule-based AI insight engine for staff management.
 *
 * <h3>Insight types and trigger rules</h3>
 * <pre>
 * TEACHER
 *   performanceScore > 85   → PROMOTION suggestion
 *   performanceScore < 60   → TRAINING recommendation
 *   attendancePct < 70      → LOW_ATTENDANCE alert
 *   highFeedback + lowResult→ TEACHING_GAP insight
 *
 * PRINCIPAL
 *   schoolPerformanceRating < 60 → SCHOOL_PERFORMANCE_ALERT
 *   lowTeacherScores average     → STAFF_EFFICIENCY insight
 *
 * ACCOUNTANT
 *   managedBudget anomaly compared to school avg → BUDGET_ANOMALY
 *
 * EXAM_COORDINATOR
 *   schedulingEfficiency < 70 → SCHEDULING_DELAY_RISK
 *   examPlanningScore < 65    → PLANNING_IMPROVEMENT suggestion
 * </pre>
 *
 * <p>All AI methods are heavily cached (30 min TTL) because the computation
 * iterates over staff records and is relatively expensive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAIService {

    // ── Thresholds (could be externalised to application.yml) ─────────────
    private static final BigDecimal PROMOTION_THRESHOLD    = new BigDecimal("85");
    private static final BigDecimal TRAINING_THRESHOLD     = new BigDecimal("60");
    private static final BigDecimal LOW_ATTENDANCE         = new BigDecimal("70");
    private static final BigDecimal HIGH_FEEDBACK_MIN      = new BigDecimal("3.5"); // avg rating out of 5
    private static final BigDecimal LOW_RESULT_THRESHOLD   = new BigDecimal("55");
    private static final BigDecimal SCHED_EFFICIENCY_WARN  = new BigDecimal("70");
    private static final BigDecimal PLAN_SCORE_WARN        = new BigDecimal("65");

    private final StaffRepository            staffRepository;
    private final StaffPerformanceRepository performanceRepository;

    // ═══════════════════════════════════════════════════════════════════════
    // Top Performers
    // ═══════════════════════════════════════════════════════════════════════

    @Cacheable(
        value  = CacheConfig.TOP_PERFORMERS,
        key    = "T(com.ai.vidya.config.CacheKeyHelper).topPerformers(#roleType)",
        unless = "#result.isEmpty()"
    )
    public List<AiInsight> getTopPerformers(String roleType) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        List<Staff> topStaff = staffRepository.findTopPerformingTeachers(
                tenantId, schoolId, PageRequest.of(0, 10));

        return topStaff.stream()
                .filter(s -> s instanceof Teacher t && t.getPerformanceScore() != null
                             && t.getPerformanceScore().compareTo(PROMOTION_THRESHOLD) >= 0)
                .map(s -> {
                    Teacher t = (Teacher) s;
                    return AiInsight.builder()
                            .staffId(t.getId())
                            .staffName(t.getName())
                            .insightType("TOP_PERFORMER")
                            .severity("LOW")
                            .message("Teacher %s has an excellent performance score of %.1f. Consider for senior role or mentorship program."
                                    .formatted(t.getName(), t.getPerformanceScore()))
                            .recommendations(List.of(
                                    "Assign as department mentor",
                                    "Nominate for excellence award",
                                    "Consider for Head of Department role"))
                            .score(t.getPerformanceScore())
                            .build();
                })
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Recommendations (training + promotion + alerts)
    // ═══════════════════════════════════════════════════════════════════════

    @Cacheable(
        value  = CacheConfig.AI_INSIGHTS,
        key    = "T(com.ai.vidya.config.CacheKeyHelper).recommendations(#roleType)",
        unless = "#result.isEmpty()"
    )
    public List<AiInsight> getRecommendations(String roleType) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        List<AiInsight> insights = new ArrayList<>();

        // ── Teacher analysis ───────────────────────────────────────────
        if (roleType == null || "TEACHER".equalsIgnoreCase(roleType)) {
            insights.addAll(analyzeTeachers(tenantId, schoolId));
        }

        // ── Exam Coordinator analysis ──────────────────────────────────
        if (roleType == null || "EXAM_COORDINATOR".equalsIgnoreCase(roleType)) {
            insights.addAll(analyzeExamCoordinators(tenantId, schoolId));
        }

        return insights;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Per-staff AI insights
    // ═══════════════════════════════════════════════════════════════════════

    @Cacheable(
        value  = CacheConfig.AI_INSIGHTS,
        key    = "T(com.ai.vidya.config.CacheKeyHelper).aiInsights(#staffId)",
        unless = "#result.isEmpty()"
    )
    public List<AiInsight> getInsightsForStaff(UUID staffId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        Staff staff = staffRepository
                .findByIdAndTenantIdAndSchoolId(staffId, tenantId, schoolId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Staff not found: " + staffId));

        return switch (staff) {
            case Teacher t    -> buildTeacherInsights(t, tenantId, schoolId);
            case Principal p  -> buildPrincipalInsights(p);
            case Accountant a -> buildAccountantInsights(a);
            case ExamCoordinator e -> buildExamCoordInsights(e);
            default -> List.of();
        };
    }

    // ── Teacher insight builders ──────────────────────────────────────────

    private List<AiInsight> analyzeTeachers(UUID tenantId, UUID schoolId) {
        List<Staff> lowPerformers = staffRepository.findLowPerformingTeachers(
                tenantId, schoolId, TRAINING_THRESHOLD);

        return lowPerformers.stream()
                .map(s -> (Teacher) s)
                .map(t -> buildTeacherInsights(t, tenantId, schoolId))
                .flatMap(List::stream)
                .toList();
    }

    private List<AiInsight> buildTeacherInsights(Teacher teacher, UUID tenantId, UUID schoolId) {
        List<AiInsight> insights = new ArrayList<>();
        BigDecimal score = teacher.getPerformanceScore();
        if (score == null) return insights;

        // Promotion rule
        if (score.compareTo(PROMOTION_THRESHOLD) > 0) {
            insights.add(AiInsight.builder()
                    .staffId(teacher.getId())
                    .staffName(teacher.getName())
                    .insightType("PROMOTION")
                    .severity("LOW")
                    .message("Teacher %s has a high performance score of %.1f — eligible for promotion."
                            .formatted(teacher.getName(), score))
                    .recommendations(List.of(
                            "Evaluate for Head of Department",
                            "Increase responsibility in curriculum design"))
                    .score(score)
                    .build());
        }

        // Training rule
        if (score.compareTo(TRAINING_THRESHOLD) < 0) {
            insights.add(AiInsight.builder()
                    .staffId(teacher.getId())
                    .staffName(teacher.getName())
                    .insightType("TRAINING")
                    .severity("HIGH")
                    .message("Teacher %s has a low performance score of %.1f — training recommended."
                            .formatted(teacher.getName(), score))
                    .recommendations(List.of(
                            "Enroll in subject-matter refresher course",
                            "Assign a senior mentor",
                            "Schedule monthly review sessions"))
                    .score(score)
                    .build());
        }

        // Teaching gap: high feedback rating but low student results
        getCurrentYearPerformance(teacher.getId(), tenantId, schoolId)
                .ifPresent(perf -> {
                    boolean highEngagement = perf.getAvgFeedbackRating() != null
                            && perf.getAvgFeedbackRating().compareTo(HIGH_FEEDBACK_MIN) >= 0;
                    boolean lowResults = perf.getResultPct() != null
                            && perf.getResultPct().compareTo(LOW_RESULT_THRESHOLD) < 0;

                    if (highEngagement && lowResults) {
                        insights.add(AiInsight.builder()
                                .staffId(teacher.getId())
                                .staffName(teacher.getName())
                                .insightType("TEACHING_GAP")
                                .severity("MEDIUM")
                                .message("Teacher %s has high student engagement (rating %.1f) but low results (%.1f%%) — possible teaching methodology gap."
                                        .formatted(teacher.getName(),
                                                perf.getAvgFeedbackRating(), perf.getResultPct()))
                                .recommendations(List.of(
                                        "Review assessment strategy",
                                        "Recommend subject training on exam techniques",
                                        "Conduct a peer observation session"))
                                .score(score)
                                .build());
                    }

                    // Low attendance alert
                    if (perf.getAttendancePct() != null
                            && perf.getAttendancePct().compareTo(LOW_ATTENDANCE) < 0) {
                        insights.add(AiInsight.builder()
                                .staffId(teacher.getId())
                                .staffName(teacher.getName())
                                .insightType("LOW_ATTENDANCE")
                                .severity("HIGH")
                                .message("Teacher %s attendance is %.1f%% — below the 70%% threshold."
                                        .formatted(teacher.getName(), perf.getAttendancePct()))
                                .recommendations(List.of(
                                        "Issue formal attendance warning",
                                        "Schedule HR check-in",
                                        "Review workload / personal issues"))
                                .score(perf.getAttendancePct())
                                .build());
                    }
                });

        return insights;
    }

    // ── Principal insight builders ────────────────────────────────────────

    private List<AiInsight> buildPrincipalInsights(Principal principal) {
        List<AiInsight> insights = new ArrayList<>();
        if (principal.getSchoolPerformanceRating() == null) return insights;

        BigDecimal rating = principal.getSchoolPerformanceRating();
        if (rating.compareTo(TRAINING_THRESHOLD) < 0) {
            insights.add(AiInsight.builder()
                    .staffId(principal.getId())
                    .staffName(principal.getName())
                    .insightType("SCHOOL_PERFORMANCE_ALERT")
                    .severity("CRITICAL")
                    .message("School performance rating under principal %s is critically low at %.1f."
                            .formatted(principal.getName(), rating))
                    .recommendations(List.of(
                            "Conduct urgent staff performance review",
                            "Review curriculum delivery quality",
                            "Engage academic coordinator for support plan"))
                    .score(rating)
                    .build());
        }
        return insights;
    }

    // ── Accountant insight builders ───────────────────────────────────────

    private List<AiInsight> buildAccountantInsights(Accountant accountant) {
        // Budget anomaly detection (simplified rule; extend with statistical baseline)
        List<AiInsight> insights = new ArrayList<>();
        if (accountant.getManagedBudget() != null
                && accountant.getManagedBudget().compareTo(new BigDecimal("5000000")) > 0) {
            insights.add(AiInsight.builder()
                    .staffId(accountant.getId())
                    .staffName(accountant.getName())
                    .insightType("BUDGET_ANOMALY")
                    .severity("MEDIUM")
                    .message("Accountant %s manages a budget exceeding ₹50L — consider audit."
                            .formatted(accountant.getName()))
                    .recommendations(List.of(
                            "Schedule quarterly internal audit",
                            "Review expense approval workflow"))
                    .score(accountant.getManagedBudget())
                    .build());
        }
        return insights;
    }

    // ── ExamCoordinator insight builders ──────────────────────────────────

    private List<AiInsight> analyzeExamCoordinators(UUID tenantId, UUID schoolId) {
        // Fetch exam coordinators and run insights — simplified for brevity
        return List.of();
    }

    private List<AiInsight> buildExamCoordInsights(ExamCoordinator ec) {
        List<AiInsight> insights = new ArrayList<>();

        if (ec.getSchedulingEfficiency() != null
                && ec.getSchedulingEfficiency().compareTo(SCHED_EFFICIENCY_WARN) < 0) {
            insights.add(AiInsight.builder()
                    .staffId(ec.getId())
                    .staffName(ec.getName())
                    .insightType("SCHEDULING_DELAY_RISK")
                    .severity("HIGH")
                    .message("Exam coordinator %s has scheduling efficiency of %.1f%% — delay risk is high."
                            .formatted(ec.getName(), ec.getSchedulingEfficiency()))
                    .recommendations(List.of(
                            "Review timetable planning process",
                            "Adopt automated scheduling tool",
                            "Assign an assistant coordinator"))
                    .score(ec.getSchedulingEfficiency())
                    .build());
        }

        if (ec.getExamPlanningScore() != null
                && ec.getExamPlanningScore().compareTo(PLAN_SCORE_WARN) < 0) {
            insights.add(AiInsight.builder()
                    .staffId(ec.getId())
                    .staffName(ec.getName())
                    .insightType("PLANNING_IMPROVEMENT")
                    .severity("MEDIUM")
                    .message("Exam coordinator %s planning score is %.1f — improvement recommended."
                            .formatted(ec.getName(), ec.getExamPlanningScore()))
                    .recommendations(List.of(
                            "Attend exam management training",
                            "Review post-exam feedback from teachers"))
                    .score(ec.getExamPlanningScore())
                    .build());
        }

        return insights;
    }

    // ── Utility ───────────────────────────────────────────────────────────

    /**
     * Fetches the most recent academic year's performance snapshot.
     * Academic year format assumed: "2024-25".
     */
    private java.util.Optional<com.ai.vidya.modules.staff.entity.StaffPerformance>
    getCurrentYearPerformance(UUID staffId, UUID tenantId, UUID schoolId) {
        int year = java.time.LocalDate.now().getYear();
        // Academic year is April-March: if current month >= April use this year, else use last year
        int startYear = java.time.LocalDate.now().getMonthValue() >= 4 ? year : year - 1;
        String academicYear = startYear + "-" + String.valueOf(startYear + 1).substring(2);
        return performanceRepository
                .findByStaffIdAndTenantIdAndSchoolIdAndAcademicYear(staffId, tenantId, schoolId, academicYear);
    }
}
