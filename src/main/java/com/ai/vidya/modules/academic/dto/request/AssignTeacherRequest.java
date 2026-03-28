package com.ai.vidya.modules.academic.dto.request;

import com.ai.vidya.modules.academic.entity.SectionSubjectTeacher;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** Assigns a teacher to teach a specific subject in a specific section. */
@Data
public class AssignTeacherRequest {

    @NotNull(message = "Class subject ID is required")
    private UUID classSubjectId;

    @NotNull(message = "Teacher ID is required")
    private UUID teacherId;

    private SectionSubjectTeacher.AssignmentType assignmentType
        = SectionSubjectTeacher.AssignmentType.THEORY;
}
