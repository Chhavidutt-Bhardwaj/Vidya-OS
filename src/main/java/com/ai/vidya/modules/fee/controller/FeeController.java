package com.ai.vidya.modules.fee.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.fee.dto.request.ApplyDiscountRequest;
import com.ai.vidya.modules.fee.dto.request.CollectFeeRequest;
import com.ai.vidya.modules.fee.dto.response.FeeDefaulterResponse;
import com.ai.vidya.modules.fee.dto.response.FeeStatementResponse;
import com.ai.vidya.modules.fee.service.FeeService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools/{schoolId}/fee")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fee Management", description = "Fee collection, instalments, discounts, and defaulters")
public class FeeController {

    private final FeeService feeService;

    @PostMapping("/students/{studentId}/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Generate fee instalments for a student on enrollment",
               description = "Call after enrolling student in a section. gradeRangeId filters the right fee structure.")
    public ResponseEntity<ApiResponse<Void>> generateInstalments(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam  UUID academicYearId,
            @RequestParam  UUID gradeRangeId) {
        feeService.generateInstalmentsForStudent(schoolId, studentId, academicYearId, gradeRangeId);
        return ResponseEntity.ok(ApiResponse.ok("Fee instalments generated successfully"));
    }

    @GetMapping("/students/{studentId}/statement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL','PARENT')")
    @Operation(summary = "Get fee statement for a student")
    public ResponseEntity<ApiResponse<FeeStatementResponse>> getStatement(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam  UUID yearId) {
        return ResponseEntity.ok(ApiResponse.ok(
            feeService.getStatement(schoolId, studentId, yearId)));
    }

    @PostMapping("/collect")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Collect fee payment",
               description = "Pass instalmentIds to pay. Payment is distributed across instalments in order.")
    public ResponseEntity<ApiResponse<String>> collect(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CollectFeeRequest request) {
        var payment = feeService.collectFee(schoolId, request);
        return ResponseEntity.ok(ApiResponse.ok(
            "Payment collected. Receipt: " + payment.getReceiptNo(), payment.getReceiptNo()));
    }

    @PostMapping("/students/{studentId}/discount")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN')")
    @Operation(summary = "Apply a discount to a student")
    public ResponseEntity<ApiResponse<Void>> applyDiscount(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam  UUID yearId,
            @Valid @RequestBody ApplyDiscountRequest request,
            @AuthenticationPrincipal VidyaUserDetails user) {
        feeService.applyDiscount(schoolId, studentId, yearId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Discount applied successfully"));
    }

    @GetMapping("/defaulters")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN','SCHOOL_ADMIN','PRINCIPAL')")
    @Operation(summary = "Get fee defaulters list",
               description = "Returns students with outstanding dues older than specified days.")
    public ResponseEntity<ApiResponse<List<FeeDefaulterResponse>>> getDefaulters(
            @PathVariable UUID schoolId,
            @RequestParam  UUID yearId,
            @RequestParam(defaultValue = "30") int overdueDays) {
        return ResponseEntity.ok(ApiResponse.ok(
            feeService.getDefaulters(schoolId, yearId, overdueDays)));
    }
}
