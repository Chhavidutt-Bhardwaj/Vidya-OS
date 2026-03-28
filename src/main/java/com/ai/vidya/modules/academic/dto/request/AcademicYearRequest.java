package com.ai.vidya.modules.academic.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request body for creating or updating an academic year.
 *
 * POST /api/v1/schools/{schoolId}/academic-years
 * PUT  /api/v1/schools/{schoolId}/academic-years/{yearId}
 *
 * At minimum, label + startDate + endDate are required.
 * Terms and shifts are optional at creation — they can be added later
 * via their own endpoints, or included here for a single-shot setup.
 */
@Data
public class AcademicYearRequest {

    /**
     * Display label — must follow YYYY-YY format.
     * e.g. "2024-25", "2025-26"
     */
    @NotBlank(message = "Academic year label is required")
    @Pattern(
        regexp  = "^\\d{4}-\\d{2}$",
        message = "Label must follow YYYY-YY format, e.g. 2024-25"
    )
    private String label;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * If true, this year becomes the current academic year.
     * The service will automatically clear the flag from any previously
     * current year for the same school.
     */
    private boolean makeCurrent = false;

    // ── Optional inline terms ─────────────────────────────────────────────

    @Valid
    private List<TermRequest> terms = new ArrayList<>();

    @Data
    public static class TermRequest {

        @NotBlank(message = "Term name is required")
        @Size(max = 50)
        private String name;

        @Min(value = 1, message = "Sort order must be at least 1")
        private int sortOrder;

        @NotNull(message = "Term start date is required")
        private LocalDate startDate;

        @NotNull(message = "Term end date is required")
        private LocalDate endDate;
    }

    // ── Optional inline shifts ────────────────────────────────────────────

    @Valid
    private List<ShiftRequest> shifts = new ArrayList<>();

    @Data
    public static class ShiftRequest {

        @NotBlank(message = "Shift name is required")
        @Size(max = 100)
        private String name;

        @NotNull(message = "Shift start time is required")
        private LocalTime startTime;

        @NotNull(message = "Shift end time is required")
        private LocalTime endTime;

        private boolean defaultShift = false;
    }
}
