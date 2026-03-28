package com.ai.vidya.modules.school.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Step 4 — Academic year, grade ranges, and shifts */
@Data
public class AcademicSetupRequest {

    // ── Academic Year ─────────────────────────────────────────────────────

    @NotBlank(message = "Academic year label is required (e.g. 2024-25)")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Label must be in format YYYY-YY e.g. 2024-25")
    private String academicYearLabel;

    @NotNull(message = "Academic year start date is required")
    private LocalDate academicYearStart;

    @NotNull(message = "Academic year end date is required")
    private LocalDate academicYearEnd;

    // ── Terms ─────────────────────────────────────────────────────────────

    @NotNull
    @Size(min = 1, message = "At least one term is required")
    @Valid
    private List<TermEntry> terms;

    @Data
    public static class TermEntry {

        @NotBlank
        @Size(max = 50)
        private String name;          // "Term 1", "Semester 1", "Q1"

        @Min(1)
        private int sortOrder;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;
    }

    // ── Shifts ────────────────────────────────────────────────────────────

    @NotNull
    @Size(min = 1, message = "At least one shift is required")
    @Valid
    private List<ShiftEntry> shifts;

    @Data
    public static class ShiftEntry {

        @NotBlank
        @Size(max = 100)
        private String name;          // "Morning Shift"

        @NotNull
        private LocalTime startTime;

        @NotNull
        private LocalTime endTime;

        private boolean defaultShift = false;
    }

    // ── Grade Ranges ──────────────────────────────────────────────────────

    @NotNull
    @Size(min = 1, message = "At least one grade range is required")
    @Valid
    private List<GradeRangeEntry> gradeRanges;

    @Data
    public static class GradeRangeEntry {

        @NotBlank
        @Size(max = 100)
        private String segmentName;   // "Pre-Primary", "Primary"

        @NotBlank
        @Size(max = 20)
        private String fromGrade;     // "Nursery", "1"

        @NotBlank
        @Size(max = 20)
        private String toGrade;       // "KG2", "5"

        @Min(0)
        private int fromGradeOrder;

        @Min(0)
        private int toGradeOrder;

        private String boardOverride;
    }
}