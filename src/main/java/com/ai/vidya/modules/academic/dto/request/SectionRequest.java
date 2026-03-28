package com.ai.vidya.modules.academic.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class SectionRequest {

    @NotBlank(message = "Section name is required")
    @Size(max = 10, message = "Section name must be at most 10 characters")
    private String name;          // "A", "B", "Morning"

    private UUID classTeacherId;  // SystemUser UUID with TEACHER role

    @Size(max = 30)
    private String room;

    @Min(1) @Max(200)
    private int capacity = 40;

    /** null = inherit default shift from academic year */
    private UUID shiftId;
}
