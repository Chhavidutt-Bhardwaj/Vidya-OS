package com.ai.vidya.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchoolDashboardResponse {

    // Student stats
    private long       totalStudents;
    private long       activeStudents;
    private long       atRiskStudents;
    private double     atRiskPercentage;
    private double     averageAttendancePct;

    // Academic stats
    private double     overallPassPercentage;
    private long       examsScheduledThisTerm;
    private long       resultsPublished;

    // Fee stats
    private BigDecimal feeCollected;
    private BigDecimal feePending;
    private BigDecimal collectionRate;
    private long       defaulterCount;

    // Staff stats
    private long       totalStaff;
    private long       staffPresentToday;
    private double     staffAttendancePct;

    // AI summary
    private int        criticalAlerts;
    private int        totalInsights;
    private String     aiSummary;
}
