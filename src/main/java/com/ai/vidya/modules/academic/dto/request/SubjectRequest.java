package com.ai.vidya.modules.academic.dto.request;

import com.ai.vidya.common.enums.SubjectType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubjectRequest {

    @NotBlank(message = "Subject code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9_-]{1,20}$",
             message = "Code must be uppercase alphanumeric (e.g. MATH, ENG-A)")
    private String code;

    @NotBlank(message = "Subject name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 30)
    private String shortName;

    @NotNull(message = "Subject type is required")
    private SubjectType subjectType;

    @Min(0) @Max(20)
    private Integer theoryPeriodsPerWeek;

    @Min(0) @Max(10)
    private Integer practicalPeriodsPerWeek = 0;

    @Min(0) @Max(200)
    private Integer maxTheoryMarks;

    @Min(0) @Max(100)
    private Integer maxPracticalMarks;

    private boolean graded = false;
    private boolean active = true;

    @Size(max = 30)
    private String boardOverride;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex e.g. #4A90D9")
    private String colorHex;
}
