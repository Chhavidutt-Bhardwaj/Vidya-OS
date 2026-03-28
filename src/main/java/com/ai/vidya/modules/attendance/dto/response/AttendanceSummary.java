package com.ai.vidya.modules.attendance.dto.response;

import com.ai.vidya.common.enums.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceSummary {
    private UUID        studentId;
    private LocalDate   fromDate;
    private LocalDate   toDate;
    private int         totalWorkingDays;
    private int         daysPresent;
    private int         daysAbsent;
    private int         daysLate;
    private int         daysOnLeave;
    private BigDecimal  attendancePercentage;
    private List<DailyRecord> dailyRecords;

    @Data @Builder
    public static class DailyRecord {
        private LocalDate        date;
        private AttendanceStatus status;
        private String           remarks;
    }
}
