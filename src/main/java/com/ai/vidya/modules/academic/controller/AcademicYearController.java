package com.ai.vidya.modules.academic.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.academic.dto.request.AcademicYearRequest;
import com.ai.vidya.modules.academic.dto.request.ShiftRequest;
import com.ai.vidya.modules.academic.dto.response.AcademicYearResponse;
import com.ai.vidya.modules.academic.service.AcademicYearService;
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
 * Academic year management endpoints.
 *
 * Base path: /api/v1/schools/{schoolId}/academic-years
 *
 * Academic Years:
 *   GET    /                              → list all for school
 *   GET    /current                       → get current year
 *   GET    /{yearId}                      → get one with terms + shifts
 *   POST   /                              → create (with optional inline terms/shifts)
 *   PUT    /{yearId}                      → update label / dates / current flag
 *   PATCH  /{yearId}/set-current          → promote to current year
 *   PATCH  /{yearId}/lock                 → lock year (irreversible)
 *   DELETE /{yearId}                      → soft delete (only if not current/locked)
 *
 * Terms (under a year):
 *   POST   /{yearId}/terms                → add term
 *   PUT    /{yearId}/terms/{termId}       → update term
 *   DELETE /{yearId}/terms/{termId}       → remove term
 *
 * Shifts (under a year):
 *   POST   /{yearId}/shifts               → add shift
 *   PUT    /{yearId}/shifts/{shiftId}     → update shift
 *   DELETE /{yearId}/shifts/{shiftId}     → remove shift
 */
@RestController
@RequestMapping("/api/v1/schools/{schoolId}/academic-years")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Academic Years", description = "Academic year, term, and shift management")
public class AcademicYearController {

    private final AcademicYearService service;

    // ══════════════════════════════════════════════════════════════════════
    // ACADEMIC YEARS
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(summary = "List all academic years for a school")
    public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> list(
            @PathVariable UUID schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listBySchool(schoolId)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Get the current academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getCurrent(
            @PathVariable UUID schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getCurrent(schoolId)));
    }

    @GetMapping("/{yearId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Get one academic year with all terms and shifts")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getById(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(schoolId, yearId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Create a new academic year",
        description = "Optionally include inline terms and shifts for single-shot setup. " +
                      "Set makeCurrent=true to immediately promote this year."
    )
    public ResponseEntity<ApiResponse<AcademicYearResponse>> create(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AcademicYearRequest request) {
        AcademicYearResponse response = service.create(schoolId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Academic year created", response));
    }

    @PutMapping("/{yearId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Update an academic year's label, dates, or current flag")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> update(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Academic year updated", service.update(schoolId, yearId, request)));
    }

    @PatchMapping("/{yearId}/set-current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Promote an academic year to current",
        description = "Clears the current flag from any other year for this school first."
    )
    public ResponseEntity<ApiResponse<AcademicYearResponse>> setCurrent(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Academic year set as current", service.setAsCurrent(schoolId, yearId)));
    }

    @PatchMapping("/{yearId}/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Lock an academic year",
        description = "Locked years cannot be edited. This is irreversible — " +
                      "lock only after all marks, fees, and reports are finalised."
    )
    public ResponseEntity<ApiResponse<AcademicYearResponse>> lock(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Academic year locked", service.lock(schoolId, yearId)));
    }

    @DeleteMapping("/{yearId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(
        summary     = "Delete an academic year",
        description = "Soft delete. Fails if the year is current or locked."
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId) {
        service.delete(schoolId, yearId);
        return ResponseEntity.ok(ApiResponse.ok("Academic year deleted"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // TERMS
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/{yearId}/terms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Add a term to an academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse.TermResponse>> addTerm(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @Valid @RequestBody AcademicYearRequest.TermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Term added", service.addTerm(schoolId, yearId, request)));
    }

    @PutMapping("/{yearId}/terms/{termId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Update a term")
    public ResponseEntity<ApiResponse<AcademicYearResponse.TermResponse>> updateTerm(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @PathVariable UUID termId,
            @Valid @RequestBody AcademicYearRequest.TermRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Term updated", service.updateTerm(schoolId, yearId, termId, request)));
    }

    @DeleteMapping("/{yearId}/terms/{termId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Delete a term")
    public ResponseEntity<ApiResponse<Void>> deleteTerm(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @PathVariable UUID termId) {
        service.deleteTerm(schoolId, yearId, termId);
        return ResponseEntity.ok(ApiResponse.ok("Term deleted"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SHIFTS
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/{yearId}/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Add a shift to an academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse.ShiftResponse>> addShift(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Shift added", service.addShift(schoolId, yearId, request)));
    }

    @PutMapping("/{yearId}/shifts/{shiftId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Update a shift")
    public ResponseEntity<ApiResponse<AcademicYearResponse.ShiftResponse>> updateShift(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @PathVariable UUID shiftId,
            @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Shift updated", service.updateShift(schoolId, yearId, shiftId, request)));
    }

    @DeleteMapping("/{yearId}/shifts/{shiftId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Delete a shift")
    public ResponseEntity<ApiResponse<Void>> deleteShift(
            @PathVariable UUID schoolId,
            @PathVariable UUID yearId,
            @PathVariable UUID shiftId) {
        service.deleteShift(schoolId, yearId, shiftId);
        return ResponseEntity.ok(ApiResponse.ok("Shift deleted"));
    }
}