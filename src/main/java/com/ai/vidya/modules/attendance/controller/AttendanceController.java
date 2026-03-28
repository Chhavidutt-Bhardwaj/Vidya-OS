package com.ai.vidya.modules.attendance.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.attendance.dto.request.BulkAttendanceRequest;
import com.ai.vidya.modules.attendance.dto.request.StaffBulkAttendanceRequest;
import com.ai.vidya.modules.attendance.dto.response.AttendanceSummary;
import com.ai.vidya.modules.attendance.dto.response.SectionAttendanceResponse;
import com.ai.vidya.modules.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools/{schoolId}/attendance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance", description = "Student and staff attendance management")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ── Student attendance ────────────────────────────────────────────────

    @PostMapping("/student/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Mark bulk student attendance for a section",
               description = "Marks daily (period=0) or period-wise attendance. " +
                             "Supports re-marking (correction). Today's date is used if date is null.")
    public ResponseEntity<ApiResponse<Void>> markStudentAttendance(
            @PathVariable UUID schoolId,
            @Valid @RequestBody BulkAttendanceRequest request) {
        attendanceService.markBulkStudentAttendance(schoolId, request);
        return ResponseEntity.ok(ApiResponse.ok("Attendance marked successfully"));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER','PARENT')")
    @Operation(summary = "Get student attendance summary for a date range")
    public ResponseEntity<ApiResponse<AttendanceSummary>> getStudentSummary(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam  UUID yearId,
            @RequestParam  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.getStudentSummary(studentId, yearId, from, to)));
    }

    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Get section attendance for a specific date and period")
    public ResponseEntity<ApiResponse<SectionAttendanceResponse>> getSectionAttendance(
            @PathVariable UUID schoolId,
            @PathVariable UUID sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int period) {
        return ResponseEntity.ok(ApiResponse.ok(
            attendanceService.getSectionAttendance(sectionId, date, period)));
    }

    // ── Staff attendance ──────────────────────────────────────────────────

    @PostMapping("/staff/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Mark bulk staff attendance")
    public ResponseEntity<ApiResponse<Void>> markStaffAttendance(
            @PathVariable UUID schoolId,
            @Valid @RequestBody StaffBulkAttendanceRequest request) {
        attendanceService.markBulkStaffAttendance(schoolId, request);
        return ResponseEntity.ok(ApiResponse.ok("Staff attendance marked successfully"));
    }
}
