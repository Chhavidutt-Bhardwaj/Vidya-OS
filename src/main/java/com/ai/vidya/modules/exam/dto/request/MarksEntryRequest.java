package com.ai.vidya.modules.exam.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class MarksEntryRequest {

    @NotNull private UUID examScheduleId;
    @NotNull private UUID sectionId;
    @NotNull private UUID enteredBy;

    @NotNull @Size(min = 1)
    @Valid
    private List<StudentMarks> marks;

    @Data
    public static class StudentMarks {
        @NotNull private UUID studentId;
        @NotNull private String subjectCode;

        @DecimalMin("0") private BigDecimal theoryMarks;
        @DecimalMin("0") private BigDecimal practicalMarks;
        @NotNull         private BigDecimal maxMarks;

        private boolean absent = false;
        private String  remarks;
    }
}
