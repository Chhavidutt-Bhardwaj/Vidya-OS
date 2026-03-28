package com.ai.vidya.modules.staff.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// ═══════════════════════════════════════════════════════════════════════════
// PerformanceResponse
// ═══════════════════════════════════════════════════════════════════════════
@Data
@Builder
public class PerformanceResponse {
    private UUID staffId;
    private String academicYear;
    private BigDecimal attendancePct;
    private BigDecimal resultPct;
    private BigDecimal avgFeedbackRating;
    private BigDecimal overallScore;
    private LocalDate periodStart;
    private LocalDate periodEnd;
}

// ═══════════════════════════════════════════════════════════════════════════
// AiInsightResponse  — returned from AI endpoints
// ═══════════════════════════════════════════════════════════════════════════
@Data
@Builder
class AiInsightResponse {
    private UUID staffId;
    private String staffName;
    private String insightType;       // PROMOTION | TRAINING | ALERT | ANOMALY | SCHEDULING
    private String severity;          // LOW | MEDIUM | HIGH | CRITICAL
    private String message;
    private List<String> recommendations;
    private BigDecimal score;
}
