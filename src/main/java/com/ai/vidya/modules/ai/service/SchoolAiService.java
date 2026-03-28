package com.ai.vidya.modules.ai.service;

import com.ai.vidya.modules.ai.dto.AiInsightResponse;
import com.ai.vidya.modules.ai.dto.SchoolDashboardResponse;
import com.ai.vidya.modules.attendance.repository.StudentAttendanceRepository;
import com.ai.vidya.modules.exam.repository.ExamResultRepository;
import com.ai.vidya.modules.fee.repository.FeeInstalmentRepository;
import com.ai.vidya.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Rule-based AI analytics engine.
 *
 * Extension points (annotated with TODO for ML upgrade):
 * - Replace threshold rules with XGBoost prediction via Python REST API
 * - Add embedding-based student similarity for peer comparison
 * - Plug in Google Vertex AI for NLP-based remark generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolAiService {

    private final StudentRepository           studentRepository;
    private final StudentAttendanceRepository attendanceRepository;
    private final ExamResultRepository        examResultRepository;
    private final FeeInstalmentRepository     feeInstalmentRepository;

    // ── Thresholds (externalise to application.yml in production) ─────────
    private static final double     ATTENDANCE_RISK_THRESHOLD  = 75.0;
    private static final BigDecimal PERFORMANCE_RISK_THRESHOLD = BigDecimal.valueOf(40.0);
    private static final int        FEE_OVERDUE_DAYS           = 30;

    // ══════════════════════════════════════════════════════════════════════
    // STUDENT RISK INSIGHTS
    // ══════════════════════════════════════════════════════════════════════

    @Async
    public CompletableFuture<List<AiInsightResponse>> getAtRiskStudentInsights(
            UUID schoolId, UUID yearId) {

        List<AiInsightResponse> insights = new ArrayList<>();

        // 1. Attendance risk
        try {
            studentRepository.findAtRiskStudents(schoolId, yearId, ATTENDANCE_RISK_THRESHOLD)
                .forEach(s -> insights.add(AiInsightResponse.builder()
                    .entityId(s.getId())
                    .entityName(s.getFullName())
                    .entityType("STUDENT")
                    .insightType("ATTENDANCE_RISK")
                    .severity("HIGH")
                    .message(String.format(
                        "%s's attendance is below %.0f%%. Immediate intervention needed.",
                        s.getFullName(), ATTENDANCE_RISK_THRESHOLD))
                    .recommendations(List.of(
                        "Send SMS/email alert to parents",
                        "Schedule counselor meeting",
                        "Verify absence reasons and document leave"))
                    .build()));
        } catch (Exception e) {
            log.warn("Failed to fetch attendance risk students: {}", e.getMessage());
        }

        // 2. Academic performance risk
        try {
            examResultRepository.findLowPerformingStudents(schoolId, yearId, PERFORMANCE_RISK_THRESHOLD)
                .forEach(r -> {
                    Object sid = r.get("studentId");
                    Object avg = r.get("averagePercentage");
                    if (sid == null) return;
                    insights.add(AiInsightResponse.builder()
                        .entityId(UUID.fromString(sid.toString()))
                        .entityType("STUDENT")
                        .insightType("PERFORMANCE_RISK")
                        .severity("MEDIUM")
                        .message(String.format(
                            "Student average is %.1f%% — below pass threshold of 40%%.",
                            avg != null ? ((Number)avg).doubleValue() : 0.0))
                        .recommendations(List.of(
                            "Arrange extra coaching sessions",
                            "Review learning difficulties",
                            "Parent-teacher conference"))
                        .score(avg != null ? BigDecimal.valueOf(((Number)avg).doubleValue()) : null)
                        .build());
                });
        } catch (Exception e) {
            log.warn("Failed to fetch performance risk students: {}", e.getMessage());
        }

        // 3. Fee default risk
        try {
            feeInstalmentRepository.findHighValueDefaulters(schoolId, yearId, FEE_OVERDUE_DAYS)
                .forEach(d -> {
                    Object sid = d.get("studentId");
                    Object amt = d.get("overdueAmount");
                    if (sid == null) return;
                    insights.add(AiInsightResponse.builder()
                        .entityId(UUID.fromString(sid.toString()))
                        .entityType("STUDENT")
                        .insightType("FEE_DEFAULT_RISK")
                        .severity("HIGH")
                        .message(String.format("Overdue fee of ₹%.0f for %d+ days.",
                            amt != null ? ((Number)amt).doubleValue() : 0.0, FEE_OVERDUE_DAYS))
                        .recommendations(List.of(
                            "Contact parents immediately",
                            "Offer payment instalment plan",
                            "Check scholarship eligibility"))
                        .score(amt != null ? BigDecimal.valueOf(((Number)amt).doubleValue()) : null)
                        .build());
                });
        } catch (Exception e) {
            log.warn("Failed to fetch fee default risk: {}", e.getMessage());
        }

        log.info("AI insights generated: {} for school {}", insights.size(), schoolId);
        return CompletableFuture.completedFuture(insights);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCHOOL DASHBOARD ANALYTICS
    // ══════════════════════════════════════════════════════════════════════

    public SchoolDashboardResponse getDashboard(UUID schoolId, UUID yearId) {

        long totalStudents = studentRepository.countActiveBySchoolId(schoolId);
        long atRisk        = studentRepository.findAtRiskStudents(schoolId, yearId, ATTENDANCE_RISK_THRESHOLD).size();

        BigDecimal feeCollected = feeInstalmentRepository.sumCollectedBySchoolAndYear(schoolId, yearId);
        BigDecimal feePending   = feeInstalmentRepository.sumPendingBySchoolAndYear(schoolId, yearId);

        double collectionRate = (feeCollected.add(feePending).compareTo(BigDecimal.ZERO) == 0) ? 0.0
            : feeCollected.doubleValue() / feeCollected.add(feePending).doubleValue() * 100;

        long defaulterCount = feeInstalmentRepository
            .findDefaulters(schoolId, yearId, LocalDate.now().minusDays(FEE_OVERDUE_DAYS)).size();

        List<?> atRiskAcademic = examResultRepository
            .findLowPerformingStudents(schoolId, yearId, PERFORMANCE_RISK_THRESHOLD);

        int totalInsights = (int) atRisk + atRiskAcademic.size() + (int) defaulterCount;
        int criticalAlerts = (int) atRisk + (int) defaulterCount;

        String summary = buildAiSummary(atRisk, atRiskAcademic.size(), defaulterCount, collectionRate);

        return SchoolDashboardResponse.builder()
            .totalStudents(totalStudents)
            .activeStudents(totalStudents)
            .atRiskStudents(atRisk)
            .atRiskPercentage(totalStudents > 0 ? (double) atRisk / totalStudents * 100 : 0.0)
            .feeCollected(feeCollected)
            .feePending(feePending)
            .collectionRate(BigDecimal.valueOf(collectionRate))
            .defaulterCount(defaulterCount)
            .criticalAlerts(criticalAlerts)
            .totalInsights(totalInsights)
            .aiSummary(summary)
            .build();
    }

    private String buildAiSummary(long attendanceRisk, int academicRisk, long feeRisk, double collectionRate) {
        if (attendanceRisk == 0 && academicRisk == 0 && feeRisk == 0) {
            return "✅ School health looks good. No critical alerts today.";
        }
        var sb = new StringBuilder();
        if (attendanceRisk > 0) sb.append(String.format("⚠️ %d students below 75%% attendance. ", attendanceRisk));
        if (academicRisk   > 0) sb.append(String.format("📉 %d students at academic risk. ", academicRisk));
        if (feeRisk        > 0) sb.append(String.format("💰 %d fee defaulters need follow-up. ", feeRisk));
        if (collectionRate < 70) sb.append("💡 Fee collection rate is below 70%. Consider targeted outreach.");
        return sb.toString().trim();
    }
}
