package com.ai.vidya.modules.academic.dto.response;

import com.ai.vidya.modules.academic.entity.ClassSubject;
import com.ai.vidya.modules.academic.entity.SectionSubjectTeacher;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassResponse {

    private UUID   id;
    private UUID   schoolId;
    private UUID   academicYearId;
    private String academicYearLabel;
    private String name;
    private String displayName;
    private int    gradeOrder;
    private String room;
    private boolean active;
    private int    sectionCount;
    private int    totalStudents;

    private List<SectionResponse>      sections;
    private List<ClassSubjectResponse> subjects;

    // ── Nested: Section ────────────────────────────────────────────────────

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SectionResponse {
        private UUID    id;
        private String  name;
        private String  fullName;          // "Class 10 - A"
        private UUID    classTeacherId;
        private String  classTeacherName;  // resolved by service
        private String  room;
        private int     capacity;
        private int     studentCount;
        private int     availableSeats;
        private boolean full;
        private boolean active;
        private UUID    shiftId;
        private String  shiftName;
        private List<TeacherAssignmentResponse> teacherAssignments;
    }

    // ── Nested: ClassSubject ───────────────────────────────────────────────

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassSubjectResponse {
        private UUID    id;
        private UUID    subjectId;
        private String  subjectCode;
        private String  subjectName;
        private String  subjectType;
        private ClassSubject.OfferingType offeringType;
        private int     theoryPeriodsPerWeek;
        private int     practicalPeriodsPerWeek;
        private int     maxTheoryMarks;
        private int     maxPracticalMarks;
        private int     totalMaxMarks;
        private String  colorHex;
    }

    // ── Nested: Teacher assignment ─────────────────────────────────────────

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TeacherAssignmentResponse {
        private UUID   id;
        private UUID   classSubjectId;
        private String subjectName;
        private UUID   teacherId;
        private String teacherName;       // resolved by service
        private SectionSubjectTeacher.AssignmentType assignmentType;
        private boolean active;
    }
}
