package com.ai.vidya.modules.staff.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.staff.ai.AiInsight;
import com.ai.vidya.modules.staff.ai.StaffAIService;
import com.ai.vidya.modules.staff.dto.request.CreateStaffRequest;
import com.ai.vidya.modules.staff.dto.request.FeedbackRequest;
import com.ai.vidya.modules.staff.dto.request.UpdateStaffRequest;
import com.ai.vidya.modules.staff.dto.response.PerformanceResponse;
import com.ai.vidya.modules.staff.dto.response.StaffResponse;
import com.ai.vidya.modules.staff.entity.StaffRoleType;
import com.ai.vidya.modules.staff.service.PerformanceService;
import com.ai.vidya.modules.staff.service.StaffService;
import com.ai.vidya.security.annotation.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Staff Management REST controller.
 *
 * <p>All endpoints are protected by:
 * <ol>
 *   <li>JWT authentication (Spring Security filter).</li>
 *   <li>Permission check via {@link CheckPermission} AOP aspect.</li>
 *   <li>Multi-tenant isolation via {@link com.ai.vidya.tenant.TenantContext}.</li>
 * </ol>
 *
 * <h3>Base path: /api/v1/staff</h3>
 */
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "CRUD, performance, and AI insight APIs for all staff roles")
public class StaffController {

    private final StaffService      staffService;
    private final PerformanceService performanceService;
    private final StaffAIService    aiService;

    // ═══════════════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping
    @CheckPermission("STAFF:CREATE")
    @Operation(summary = "Create a new staff member")
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {

        StaffResponse response = staffService.createStaff(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff created successfully", response));
    }

    @GetMapping
    @CheckPermission("STAFF:READ")
    @Operation(summary = "List all staff (paginated, optional role filter)")
    public ResponseEntity<ApiResponse<Page<StaffResponse>>> getAllStaff(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    StaffRoleType roleType) {

        return ResponseEntity.ok(
                ApiResponse.success(staffService.getAllStaff(page, size, roleType)));
    }

    @GetMapping("/{id}")
    @CheckPermission("STAFF:READ")
    @Operation(summary = "Get a single staff member by ID")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaffById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(staffService.getStaffById(id)));
    }

    @PutMapping("/{id}")
    @CheckPermission("STAFF:UPDATE")
    @Operation(summary = "Update a staff member")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Staff updated successfully", staffService.updateStaff(id, request)));
    }

    @DeleteMapping("/{id}")
    @CheckPermission("STAFF:DELETE")
    @Operation(summary = "Soft-delete a staff member")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(
            @PathVariable UUID id,
            Authentication auth) {

        staffService.deleteStaff(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Staff deleted successfully", null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Performance APIs
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/feedback")
    @CheckPermission("STAFF:UPDATE")
    @Operation(summary = "Submit feedback for a staff member")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackRequest request) {

        performanceService.submitFeedback(id, request);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted", null));
    }

    @GetMapping("/{id}/performance")
    @CheckPermission("TEACHER:PERFORMANCE_VIEW")
    @Operation(summary = "Get performance snapshot for a staff member")
    public ResponseEntity<ApiResponse<PerformanceResponse>> getPerformance(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "2024-25") String academicYear) {

        return ResponseEntity.ok(
                ApiResponse.success(performanceService.getPerformance(id, academicYear)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AI APIs
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/top-performers")
    @CheckPermission("AI:INSIGHTS")
    @Operation(summary = "Get AI-identified top-performing staff")
    public ResponseEntity<ApiResponse<List<AiInsight>>> getTopPerformers(
            @RequestParam(required = false) String roleType) {

        return ResponseEntity.ok(
                ApiResponse.success(aiService.getTopPerformers(roleType)));
    }

    @GetMapping("/recommendations")
    @CheckPermission("AI:INSIGHTS")
    @Operation(summary = "Get AI-generated staff recommendations (training, promotion, alerts)")
    public ResponseEntity<ApiResponse<List<AiInsight>>> getRecommendations(
            @RequestParam(required = false) String roleType) {

        return ResponseEntity.ok(
                ApiResponse.success(aiService.getRecommendations(roleType)));
    }

    @GetMapping("/{id}/insights")
    @CheckPermission("AI:INSIGHTS")
    @Operation(summary = "Get AI insights for a specific staff member")
    public ResponseEntity<ApiResponse<List<AiInsight>>> getInsightsForStaff(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success(aiService.getInsightsForStaff(id)));
    }
}
