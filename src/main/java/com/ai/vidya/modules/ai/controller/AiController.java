package com.ai.vidya.modules.ai.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.ai.dto.AiInsightResponse;
import com.ai.vidya.modules.ai.dto.SchoolDashboardResponse;
import com.ai.vidya.modules.ai.service.SchoolAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/schools/{schoolId}/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Analytics", description = "AI-powered insights for student risk, staff performance, and school analytics")
public class AiController {

    private final SchoolAiService aiService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(summary = "Get AI-powered school dashboard",
               description = "Returns aggregated analytics: student risk, fee collection, staff health, and AI summary.")
    public ResponseEntity<ApiResponse<SchoolDashboardResponse>> getDashboard(
            @PathVariable UUID schoolId,
            @RequestParam  UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.getDashboard(schoolId, yearId)));
    }

    @GetMapping("/students/at-risk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(summary = "Get at-risk student insights",
               description = "Returns students flagged for attendance risk, performance risk, and fee default risk.")
    public ResponseEntity<ApiResponse<List<AiInsightResponse>>> getAtRiskStudents(
            @PathVariable UUID schoolId,
            @RequestParam  UUID yearId) throws ExecutionException, InterruptedException {
        List<AiInsightResponse> insights = aiService.getAtRiskStudentInsights(schoolId, yearId).get();
        return ResponseEntity.ok(ApiResponse.ok(insights));
    }
}
