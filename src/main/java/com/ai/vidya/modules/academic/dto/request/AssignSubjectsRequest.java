package com.ai.vidya.modules.academic.dto.request;

import com.ai.vidya.modules.academic.entity.ClassSubject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Assigns one or more subjects to a class in bulk. */
@Data
public class AssignSubjectsRequest {

    @NotNull
    @Size(min = 1, message = "At least one subject assignment is required")
    @Valid
    private List<SubjectAssignment> assignments;

    @Data
    public static class SubjectAssignment {

        @NotNull(message = "Subject ID is required")
        private UUID subjectId;

        private ClassSubject.OfferingType offeringType = ClassSubject.OfferingType.COMPULSORY;

        /** Overrides Subject.theoryPeriodsPerWeek for this class */
        @Min(0) @Max(20)
        private Integer theoryPeriodsPerWeek;

        @Min(0) @Max(10)
        private Integer practicalPeriodsPerWeek;

        @Min(0) @Max(200)
        private Integer maxTheoryMarks;

        @Min(0) @Max(100)
        private Integer maxPracticalMarks;
    }
}
