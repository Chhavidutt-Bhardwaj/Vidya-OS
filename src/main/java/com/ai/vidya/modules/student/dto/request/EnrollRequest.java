package com.ai.vidya.modules.student.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EnrollRequest {

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    @NotNull(message = "Section ID is required")
    private UUID sectionId;

    private String rollNo;
}
