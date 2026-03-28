package com.ai.vidya.modules.staff.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Immutable AI insight result returned by {@link StaffAIService}.
 *
 * <p>This is a pure data carrier — it is serialised to JSON and cached.
 * It must implement {@link java.io.Serializable} if Redis serialisation is enabled.
 */
@Data
@Builder
public class AiInsight implements java.io.Serializable {

    private UUID   staffId;
    private String staffName;

    /**
     * One of: PROMOTION | TRAINING | LOW_ATTENDANCE | TEACHING_GAP |
     * SCHOOL_PERFORMANCE_ALERT | BUDGET_ANOMALY | SCHEDULING_DELAY_RISK |
     * PLANNING_IMPROVEMENT | TOP_PERFORMER | STAFF_EFFICIENCY
     */
    private String insightType;

    /** LOW | MEDIUM | HIGH | CRITICAL */
    private String severity;

    /** Human-readable insight message, ready for UI display. */
    private String message;

    /** Ordered list of concrete action items. */
    private List<String> recommendations;

    /** The score or metric that triggered this insight. */
    private BigDecimal score;
}
