package com.ai.vidya.modules.attendance.dto.response;

import com.ai.vidya.common.enums.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionAttendanceResponse {
    private UUID              sectionId;
    private LocalDate         date;
    private int               periodNumber;
    private int               totalStudents;
    private int               presentCount;
    private int               absentCount;
    private int               lateCount;
    private boolean           alreadyMarked;
    private List<StudentEntry> records;

    @Data @Builder
    public static class StudentEntry {
        private UUID             studentId;
        private String           studentName;
        private String           rollNo;
        private AttendanceStatus status;
        private String           remarks;
    }
}
