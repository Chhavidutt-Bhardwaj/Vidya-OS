package com.ai.vidya.modules.student.dto.response;

import com.ai.vidya.common.enums.BloodGroup;
import com.ai.vidya.common.enums.Gender;
import com.ai.vidya.common.enums.GuardianRelation;
import com.ai.vidya.modules.student.entity.Student;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentResponse {

    private UUID          id;
    private UUID          schoolId;
    private String        admissionNo;
    private String        fullName;
    private String        firstName;
    private String        middleName;
    private String        lastName;
    private Gender        gender;
    private LocalDate     dateOfBirth;
    private BloodGroup    bloodGroup;
    private String        photoUrl;
    private String        mobileNo;
    private String        personalEmail;
    private String        religion;
    private String        casteCategory;
    private String        nationality;
    private String        currentRollNo;
    private UUID          currentSectionId;
    private UUID          currentAcademicYearId;
    private LocalDate     admissionDate;
    private String        admissionClass;
    private Student.StudentStatus status;
    private LocalDate     leavingDate;
    private List<GuardianSummary> guardians;
    private List<EnrollmentSummary> enrollments;

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GuardianSummary {
        private String           fullName;
        private GuardianRelation relation;
        private String           mobile;
        private String           email;
        private boolean          primary;
        private boolean          canPickup;
    }

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EnrollmentSummary {
        private UUID   academicYearId;
        private UUID   sectionId;
        private String className;
        private String sectionName;
        private String rollNo;
        private String status;
        private LocalDate enrolledOn;
    }
}
