package com.ai.vidya.modules.academic.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.academic.dto.request.SubjectRequest;
import com.ai.vidya.modules.academic.dto.response.SubjectResponse;
import com.ai.vidya.modules.academic.service.SubjectService;
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
 * Subject management.
 * Base: /api/v1/schools/{schoolId}/subjects
 *
 *  GET    /          → list all subjects for school
 *  GET    /active    → list active subjects only
 *  GET    /{id}      → get one subject
 *  POST   /          → create subject
 *  PUT    /{id}      → update subject
 *  DELETE /{id}      → soft delete subject
 */
@RestController
@RequestMapping("/api/v1/schools/{schoolId}/subjects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subjects", description = "Subject definition and management per school")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "List all subjects defined for a school")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> list(
            @PathVariable UUID schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.listBySchool(schoolId)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "List active subjects — used when assigning to a class")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> listActive(
            @PathVariable UUID schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.listActiveBySchool(schoolId)));
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Get one subject by ID")
    public ResponseEntity<ApiResponse<SubjectResponse>> getById(
            @PathVariable UUID schoolId,
            @PathVariable UUID subjectId) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getById(schoolId, subjectId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Create a new subject for the school")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(
            @PathVariable UUID schoolId,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Subject created", subjectService.create(schoolId, request)));
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Update a subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable UUID schoolId,
            @PathVariable UUID subjectId,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Subject updated", subjectService.update(schoolId, subjectId, request)));
    }

    @DeleteMapping("/{subjectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Delete a subject (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID schoolId,
            @PathVariable UUID subjectId) {
        subjectService.delete(schoolId, subjectId);
        return ResponseEntity.ok(ApiResponse.ok("Subject deleted"));
    }
}
