package com.ai.vidya.modules.staff.service;

import com.ai.vidya.modules.staff.dto.request.CreateStaffRequest;
import com.ai.vidya.modules.staff.dto.response.StaffResponse;
import com.ai.vidya.modules.staff.entity.*;
import org.mapstruct.*;

/**
 * MapStruct mapper for Staff entity ↔ DTO conversions.
 *
 * <p>Uses {@code componentModel = "spring"} so the generated implementation
 * is a Spring bean injected via {@code @Autowired} / constructor injection.
 *
 * <p>The {@code toResponse} method handles polymorphism with an {@code afterMapping}
 * hook that populates subtype-specific fields based on the concrete runtime type.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StaffMapper {

    // ── Entity → Response ─────────────────────────────────────────────────

    @Mapping(target = "subject",                ignore = true)
    @Mapping(target = "experienceYears",        ignore = true)
    @Mapping(target = "qualification",          ignore = true)
    @Mapping(target = "performanceScore",       ignore = true)
    @Mapping(target = "leadershipScore",        ignore = true)
    @Mapping(target = "schoolPerformanceRating",ignore = true)
    @Mapping(target = "financeAccessLevel",     ignore = true)
    @Mapping(target = "managedBudget",          ignore = true)
    @Mapping(target = "schedulingEfficiency",   ignore = true)
    @Mapping(target = "examPlanningScore",      ignore = true)
    StaffResponse toResponse(Staff staff);

    /**
     * After the base mapping completes, populate subtype-specific fields
     * by checking the actual runtime type using Java 21 pattern matching.
     */
    @AfterMapping
    default void enrichSubtypeFields(Staff staff, @MappingTarget StaffResponse.StaffResponseBuilder builder) {
        switch (staff) {
            case Teacher t -> builder
                    .subject(t.getSubject())
                    .experienceYears(t.getExperienceYears())
                    .qualification(t.getQualification())
                    .performanceScore(t.getPerformanceScore());

            case Principal p -> builder
                    .leadershipScore(p.getLeadershipScore())
                    .schoolPerformanceRating(p.getSchoolPerformanceRating());

            case Accountant a -> builder
                    .financeAccessLevel(a.getFinanceAccessLevel())
                    .managedBudget(a.getManagedBudget());

            case ExamCoordinator e -> builder
                    .schedulingEfficiency(e.getSchedulingEfficiency())
                    .examPlanningScore(e.getExamPlanningScore());

            default -> { /* HR and others — no extra fields */ }
        }
    }

    // ── Request → Entity ──────────────────────────────────────────────────

    /**
     * Creates the correct Staff subtype from the request, based on {@code roleType}.
     * tenantId and schoolId are injected by the service after this call.
     */
    default Staff toEntity(CreateStaffRequest req) {
        return switch (req.getRoleType()) {
            case TEACHER -> Teacher.builder()
                    .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
                    .department(req.getDepartment()).joiningDate(req.getJoiningDate())
                    .salary(req.getSalary())
                    .subject(req.getSubject())
                    .experienceYears(req.getExperienceYears())
                    .qualification(req.getQualification())
                    .build();

            case PRINCIPAL -> Principal.builder()
                    .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
                    .department(req.getDepartment()).joiningDate(req.getJoiningDate())
                    .salary(req.getSalary())
                    .leadershipScore(req.getLeadershipScore())
                    .build();

            case ACCOUNTANT -> Accountant.builder()
                    .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
                    .department(req.getDepartment()).joiningDate(req.getJoiningDate())
                    .salary(req.getSalary())
                    .financeAccessLevel(req.getFinanceAccessLevel())
                    .managedBudget(req.getManagedBudget())
                    .build();

            case EXAM_COORDINATOR -> ExamCoordinator.builder()
                    .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
                    .department(req.getDepartment()).joiningDate(req.getJoiningDate())
                    .salary(req.getSalary())
                    .schedulingEfficiency(req.getSchedulingEfficiency())
                    .examPlanningScore(req.getExamPlanningScore())
                    .build();

            case HR -> HrStaff.builder()
                    .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
                    .department(req.getDepartment()).joiningDate(req.getJoiningDate())
                    .salary(req.getSalary())
                    .build();
        };
    }
}
