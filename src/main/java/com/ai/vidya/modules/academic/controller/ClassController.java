package com.ai.vidya.modules.academic.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.academic.dto.request.*;
import com.ai.vidya.modules.academic.dto.response.ClassResponse;
import com.ai.vidya.modules.academic.dto.response.ClassResponse.*;
import com.ai.vidya.modules.academic.service.ClassService;
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
 * Class, section, and teacher assignment management.
 * Base: /api/v1/schools/{schoolId}/classes
 *
 * Classes:
 *   GET    /                                                  → list by year (?yearId=)
 *   GET    /{classId}                                         → get with sections + subjects
 *   POST   /                                                  → create (inline sections/subjects)
 *   PUT    /{classId}                                         → update
 *   DELETE /{classId}                                         → soft delete
 *
 * Sections:
 *   POST   /{classId}/sections                                → add section
 *   PUT    /{classId}/sections/{sectionId}                    → update section
 *   DELETE /{classId}/sections/{sectionId}                    → remove section
 *
 * Subject assignments:
 *   POST   /{classId}/subjects                                → assign subjects to class
 *   DELETE /{classId}/subjects/{classSubjectId}               → remove subject from class
 *
 * Teacher assignments:
 *   POST   /{classId}/sections/{sectionId}/teachers           → assign teacher to subject in section
 *   DELETE /{classId}/sections/{sectionId}/teachers/{asnId}   → remove teacher assignment
 */
@RestController
@RequestMapping("/api/v1/schools/{schoolId}/classes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Classes & Sections", description = "Class, section, subject and teacher assignment management")
public class ClassController {

    private final ClassService classService;

    // ── Classes ────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(
        summary     = "List all classes for a school in an academic year",
        description = "Pass ?yearId=<uuid> to filter by year. Returns summary (no sections detail)."
    )
    public ResponseEntity<ApiResponse<List<ClassResponse>>> list(
            @PathVariable UUID schoolId,
            @RequestParam  UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(classService.listByYear(schoolId, yearId)));
    }

    @GetMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Get a class with all sections, subjects, and teacher assignments")
    public ResponseEntity<ApiResponse<ClassResponse>> getById(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.ok(classService.getById(schoolId, classId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Create a class",
        description = "Optionally create sections and assign subjects inline in the same request."
    )
    public ResponseEntity<ApiResponse<ClassResponse>> create(
            @PathVariable UUID schoolId,
            @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Class created", classService.create(schoolId, request)));
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Update a class name, display name, grade order, or room")
    public ResponseEntity<ApiResponse<ClassResponse>> update(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Class updated", classService.update(schoolId, classId, request)));
    }

    @DeleteMapping("/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Soft delete a class")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {
        classService.delete(schoolId, classId);
        return ResponseEntity.ok(ApiResponse.ok("Class deleted"));
    }

    // ── Sections ───────────────────────────────────────────────────────────

    @PostMapping("/{classId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Add a section to a class")
    public ResponseEntity<ApiResponse<SectionResponse>> addSection(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Section added",
                classService.addSection(schoolId, classId, request)));
    }

    @PutMapping("/{classId}/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Update a section")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Section updated",
            classService.updateSection(schoolId, classId, sectionId, request)));
    }

    @DeleteMapping("/{classId}/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Delete a section",
        description = "Fails if the section has enrolled students — transfer them first."
    )
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID sectionId) {
        classService.deleteSection(schoolId, classId, sectionId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted"));
    }

    // ── Subject assignments ────────────────────────────────────────────────

    @PostMapping("/{classId}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Assign subjects to a class",
        description = "Pass a list of subject IDs with optional per-class overrides " +
                      "for periods per week and max marks."
    )
    public ResponseEntity<ApiResponse<List<ClassSubjectResponse>>> assignSubjects(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @Valid @RequestBody AssignSubjectsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Subjects assigned",
                classService.assignSubjects(schoolId, classId, request)));
    }

    @DeleteMapping("/{classId}/subjects/{classSubjectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Remove a subject assignment from a class")
    public ResponseEntity<ApiResponse<Void>> removeSubject(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID classSubjectId) {
        classService.removeSubject(schoolId, classId, classSubjectId);
        return ResponseEntity.ok(ApiResponse.ok("Subject removed from class"));
    }

    // ── Teacher assignments ────────────────────────────────────────────────

    @PostMapping("/{classId}/sections/{sectionId}/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(
        summary     = "Assign a teacher to a subject in a section",
        description = "Specify classSubjectId (from the class's subject list) " +
                      "and teacherId. Only one THEORY teacher per section-subject allowed."
    )
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> assignTeacher(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody AssignTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Teacher assigned",
                classService.assignTeacher(schoolId, classId, sectionId, request)));
    }

    @DeleteMapping("/{classId}/sections/{sectionId}/teachers/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(summary = "Remove a teacher assignment from a section-subject")
    public ResponseEntity<ApiResponse<Void>> removeTeacherAssignment(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID sectionId,
            @PathVariable UUID assignmentId) {
        classService.removeTeacherAssignment(schoolId, classId, sectionId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok("Teacher assignment removed"));
    }
}
