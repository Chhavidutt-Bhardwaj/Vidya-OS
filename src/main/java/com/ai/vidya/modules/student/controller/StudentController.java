package com.ai.vidya.modules.student.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.common.response.PageResponse;
import com.ai.vidya.modules.student.dto.request.AdmissionRequest;
import com.ai.vidya.modules.student.dto.request.EnrollRequest;
import com.ai.vidya.modules.student.dto.request.PromoteStudentsRequest;
import com.ai.vidya.modules.student.dto.response.StudentResponse;
import com.ai.vidya.modules.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Student management endpoints.
 * Base: /api/v1/schools/{schoolId}/students
 */
@RestController
@RequestMapping("/api/v1/schools/{schoolId}/students")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Students", description = "Student admission, enrollment, and promotion management")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Admit a new student",
               description = "Creates student record, guardian accounts, and optionally enrolls in section.")
    public ResponseEntity<ApiResponse<StudentResponse>> admit(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AdmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Student admitted successfully",
                studentService.admit(schoolId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "List students for a school (paginated)",
               description = "Pass ?search= for name or admission number search.")
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> list(
            @PathVariable UUID schoolId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search) {
        return ResponseEntity.ok(ApiResponse.ok(
            studentService.listBySchool(schoolId, page, size, search)));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER','PARENT')")
    @Operation(summary = "Get a student with full details")
    public ResponseEntity<ApiResponse<StudentResponse>> getById(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getById(schoolId, studentId)));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "List all students in a section for a given academic year")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listBySection(
            @PathVariable UUID schoolId,
            @PathVariable UUID sectionId,
            @RequestParam  UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(
            studentService.listBySection(schoolId, sectionId, yearId)));
    }

    @PostMapping("/{studentId}/enroll")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Enroll a student in a section for an academic year")
    public ResponseEntity<ApiResponse<StudentResponse>> enroll(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @Valid @RequestBody EnrollRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Student enrolled",
            studentService.enroll(schoolId, studentId, request)));
    }

    @PostMapping("/promote")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Bulk promote or detain students at year end")
    public ResponseEntity<ApiResponse<StudentService.PromotionResult>> promote(
            @PathVariable UUID schoolId,
            @Valid @RequestBody PromoteStudentsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Promotion completed",
            studentService.promoteStudents(schoolId, request)));
    }
}
