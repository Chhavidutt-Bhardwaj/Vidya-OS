package com.ai.vidya.modules.academic.dto.response;

import com.ai.vidya.common.enums.SubjectType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

// ── Subject ────────────────────────────────────────────────────────────────

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubjectResponse {
    private UUID        id;
    private UUID        schoolId;
    private String      code;
    private String      name;
    private String      shortName;
    private SubjectType subjectType;
    private Integer     theoryPeriodsPerWeek;
    private Integer     practicalPeriodsPerWeek;
    private Integer     maxTheoryMarks;
    private Integer     maxPracticalMarks;
    private int         totalMaxMarks;
    private boolean     graded;
    private boolean     active;
    private String      boardOverride;
    private String      colorHex;
}
