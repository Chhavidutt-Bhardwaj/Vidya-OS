package com.ai.vidya.modules.academic.dto.request;

import com.ai.vidya.modules.academic.entity.ClassSubject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ClassRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 50)
    private String name;          // "Class 10", "Nursery", "KG2"

    @Size(max = 30)
    private String displayName;   // "X", "Nur" — short form for timetable

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    @Min(0) @Max(30)
    private int gradeOrder;       // 0=Nursery, 1=KG1 … 14=Class12

    @Size(max = 30)
    private String room;

    /** Create sections inline during class creation */
    @Valid
    private List<InlineSectionRequest> sections = new ArrayList<>();

    /** Assign subjects inline during class creation */
    @Valid
    private List<InlineSubjectAssignment> subjects = new ArrayList<>();

    @Data
    public static class InlineSectionRequest {
        @NotBlank @Size(max = 10) private String name;
        @Size(max = 30)           private String room;
        @Min(1) @Max(200)         private int    capacity = 40;
        private UUID shiftId;
        private UUID classTeacherId;
    }

    @Data
    public static class InlineSubjectAssignment {
        @NotNull private UUID subjectId;
        private ClassSubject.OfferingType offeringType = ClassSubject.OfferingType.COMPULSORY;
        private Integer theoryPeriodsPerWeek;
        private Integer practicalPeriodsPerWeek;
        private Integer maxTheoryMarks;
        private Integer maxPracticalMarks;
    }
}
