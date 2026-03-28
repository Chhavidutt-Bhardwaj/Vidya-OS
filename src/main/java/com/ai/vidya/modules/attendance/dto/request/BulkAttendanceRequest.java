package com.ai.vidya.modules.attendance.dto.request;

import com.ai.vidya.common.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BulkAttendanceRequest {

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    @NotNull(message = "Section ID is required")
    private UUID sectionId;

    @NotNull(message = "Teacher ID is required")
    private UUID teacherId;

    private LocalDate date;              // defaults to today if null

    private int periodNumber = 0;        // 0 = daily, 1-8 = period-wise

    @NotNull @Size(min = 1)
    @Valid
    private List<AttendanceEntry> entries;

    @Data
    public static class AttendanceEntry {
        @NotNull private UUID studentId;
        @NotNull private AttendanceStatus status;
        private String remarks;
    }
}
