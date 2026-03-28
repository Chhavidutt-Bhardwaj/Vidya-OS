package com.ai.vidya.modules.student.dto.request;

import com.ai.vidya.common.enums.BloodGroup;
import com.ai.vidya.common.enums.Gender;
import com.ai.vidya.common.enums.GuardianRelation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class AdmissionRequest {

    @Size(max = 30)
    private String admissionNo;          // null = auto-generate

    @NotBlank(message = "First name is required")
    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 80)
    private String lastName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private BloodGroup bloodGroup;

    @Pattern(regexp = "^\\d{12}$", message = "Aadhar must be 12 digits")
    private String aadharNo;

    private String religion;
    private String casteCategory;
    private String nationality;

    @Email(message = "Invalid email")
    @Size(max = 255)
    private String personalEmail;

    @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,15}$", message = "Invalid mobile number")
    private String mobileNo;

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;

    @NotBlank(message = "Admission class is required")
    private String admissionClass;

    // Optional — enroll immediately if provided
    private UUID academicYearId;
    private UUID sectionId;

    @Size(max = 20)
    private String rollNo;

    @NotNull(message = "At least one guardian is required")
    @Size(min = 1, message = "At least one guardian is required")
    @Valid
    private List<GuardianRequest> guardians = new ArrayList<>();

    @Data
    public static class GuardianRequest {

        @NotNull(message = "Guardian relation is required")
        private GuardianRelation relation;

        @NotBlank(message = "Guardian name is required")
        @Size(max = 150)
        private String fullName;

        @NotBlank(message = "Guardian mobile is required")
        @Pattern(regexp = "^[+]?[\\d\\s\\-]{7,15}$")
        private String mobile;

        @Email @Size(max = 255)
        private String email;

        @Size(max = 100)
        private String occupation;

        @Pattern(regexp = "^\\d{12}$", message = "Aadhar must be 12 digits")
        private String aadharNo;

        private Long annualIncome;
        private boolean primary    = false;
        private boolean canPickup  = true;
    }
}
