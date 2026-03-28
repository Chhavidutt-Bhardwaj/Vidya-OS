package com.ai.vidya.modules.exam.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.exam.dto.request.MarksEntryRequest;
import com.ai.vidya.modules.exam.dto.response.ReportCardResponse;
import com.ai.vidya.modules.exam.service.ExamResultService;
import com.ai.vidya.security.VidyaUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools/{schoolId}/exams")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Exam Results", description = "Marks entry, result publishing, and report card generation")
public class ExamResultController {

    private final ExamResultService examResultService;

    @PostMapping("/results/entry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    @Operation(summary = "Enter exam marks for a section",
               description = "Marks can be re-entered before publishing. After publish, marks are locked.")
    public ResponseEntity<ApiResponse<Integer>> enterMarks(
            @PathVariable UUID schoolId,
            @RequestParam  UUID yearId,
            @Valid @RequestBody MarksEntryRequest request) {
        int count = examResultService.enterMarks(schoolId, yearId, request);
        return ResponseEntity.ok(ApiResponse.ok("Marks saved for " + count + " students", count));
    }

    @PatchMapping("/results/{examScheduleId}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(summary = "Publish results for an exam schedule",
               description = "Makes results visible to students/parents. Cannot be unpublished.")
    public ResponseEntity<ApiResponse<Integer>> publishResults(
            @PathVariable UUID schoolId,
            @PathVariable UUID examScheduleId,
            @AuthenticationPrincipal VidyaUserDetails user) {
        int count = examResultService.publishResults(examScheduleId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Published results for " + count + " students", count));
    }

    @GetMapping("/students/{studentId}/report-card")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','TEACHER','PARENT','STUDENT')")
    @Operation(summary = "Generate report card for a student",
               description = "Pass termId for term report card, omit for annual report card.")
    public ResponseEntity<ApiResponse<ReportCardResponse>> getReportCard(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam  UUID yearId,
            @RequestParam(required = false) UUID termId) {
        return ResponseEntity.ok(ApiResponse.ok(
            examResultService.generateReportCard(schoolId, studentId, yearId, termId)));
    }
}
