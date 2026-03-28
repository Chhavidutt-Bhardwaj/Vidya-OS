package com.ai.vidya.modules.attendance.dto.request;

import com.ai.vidya.common.enums.StaffAttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class StaffBulkAttendanceRequest {

    @NotNull private UUID   academicYearId;
    @NotNull private UUID   markedBy;
    private LocalDate date;

    @NotNull @Size(min = 1)
    @Valid
    private List<StaffEntry> entries;

    @Data
    public static class StaffEntry {
        @NotNull private UUID                staffId;
        @NotNull private StaffAttendanceStatus status;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String    remarks;
        private String    leaveType;
    }
}
