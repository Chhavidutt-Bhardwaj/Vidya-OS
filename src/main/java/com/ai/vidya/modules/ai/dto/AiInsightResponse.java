package com.ai.vidya.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInsightResponse {

    private UUID        entityId;
    private String      entityName;

    /**
     * STUDENT | STAFF | SCHOOL
     */
    private String      entityType;

    /**
     * ATTENDANCE_RISK | PERFORMANCE_RISK | FEE_DEFAULT_RISK |
     * PROMOTION_CANDIDATE | TRAINING_NEEDED | TOP_PERFORMER |
     * SCHOOL_PERFORMANCE_ALERT | BUDGET_ANOMALY | SCHEDULING_DELAY_RISK
     */
    private String      insightType;

    /**
     * LOW | MEDIUM | HIGH | CRITICAL
     */
    private String      severity;

    private String      message;
    private List<String> recommendations;
    private BigDecimal  score;
    private String      additionalData;
}
