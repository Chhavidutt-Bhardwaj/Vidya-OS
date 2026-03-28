package com.ai.vidya.modules.academic.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Full academic year response including nested terms and shifts.
 * Returned by GET, POST, and PUT endpoints.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcademicYearResponse {

    private UUID      id;
    private UUID      schoolId;
    private String    label;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean   current;
    private boolean   locked;

    /** Whether the academic year has started (startDate <= today) */
    private boolean started;

    /** Whether the academic year has ended (endDate < today) */
    private boolean ended;

    /** Number of calendar days in the year */
    private long totalDays;

    private List<TermResponse>  terms;
    private List<ShiftResponse> shifts;

    // ── Nested DTOs ────────────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TermResponse {
        private UUID      id;
        private String    name;
        private int       sortOrder;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean   locked;
        private boolean   current;    // true if today falls within this term
        private long      totalDays;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShiftResponse {
        private UUID      id;
        private String    name;
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean   defaultShift;
        /** Formatted duration e.g. "6h 0m" */
        private String    duration;
    }
}
