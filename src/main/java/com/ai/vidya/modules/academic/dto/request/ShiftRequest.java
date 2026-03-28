package com.ai.vidya.modules.academic.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalTime;

/**
 * Standalone request for adding or updating a single shift.
 *
 * POST /api/v1/schools/{schoolId}/academic-years/{yearId}/shifts
 * PUT  /api/v1/schools/{schoolId}/academic-years/{yearId}/shifts/{shiftId}
 */
@Data
public class ShiftRequest {

    @NotBlank(message = "Shift name is required")
    @Size(max = 100, message = "Shift name must be at most 100 characters")
    private String name;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /**
     * If true, this shift becomes the default for the academic year.
     * The service clears the flag from any other shift in the same year.
     */
    private boolean defaultShift = false;
}
