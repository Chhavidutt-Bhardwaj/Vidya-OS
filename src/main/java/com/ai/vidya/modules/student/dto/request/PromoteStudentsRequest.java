package com.ai.vidya.modules.student.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PromoteStudentsRequest {

    @NotNull(message = "Source academic year ID is required")
    private UUID fromYearId;

    @NotNull(message = "Target academic year ID is required")
    private UUID toYearId;

    @NotNull @Size(min = 1)
    @Valid
    private List<StudentPromotion> students;

    @Data
    public static class StudentPromotion {
        @NotNull private UUID   studentId;
        private boolean         promoted = true;
        @NotNull private UUID   nextSectionId;
        private String          nextRollNo;
        private String          remarks;
    }
}
