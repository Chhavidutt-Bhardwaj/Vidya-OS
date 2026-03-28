package com.ai.vidya.modules.school.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.common.response.PageResponse;
import com.ai.vidya.config.CacheConfig;
import com.ai.vidya.modules.school.dto.request.BranchOnboardRequest;
import com.ai.vidya.modules.school.dto.request.ChainCreateRequest;
import com.ai.vidya.modules.school.dto.request.SchoolOnboardRequest;
import com.ai.vidya.modules.school.dto.response.BranchOnboardResponse;
import com.ai.vidya.modules.school.dto.response.ChainCreateResponse;
import com.ai.vidya.modules.school.dto.response.SchoolOnboardResponse;
import com.ai.vidya.modules.school.service.SchoolOnboardingService;
import com.ai.vidya.modules.school.service.SchoolQueryService;
import com.ai.vidya.security.VidyaUserDetails;
import com.ai.vidya.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Schools & Chains", description = "School onboarding, chain management, branch management")
public class SchoolController {

    private final SchoolOnboardingService onboardingService;
    private final SchoolQueryService queryService;

    // ══════════════════════════════════════════════════════════════════════
    // STANDALONE SCHOOL ONBOARDING
    // POST /api/v1/schools
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/schools")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Onboard a new standalone school",
               description = "Creates school, address, contact, settings, academic year and school admin user in one atomic transaction. SUPER_ADMIN only.")
    public ResponseEntity<ApiResponse<SchoolOnboardResponse>> onboardSchool(
            @Valid @RequestBody SchoolOnboardRequest request) {
        SchoolOnboardResponse response = onboardingService.onboardStandaloneSchool(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("School onboarded successfully", response));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCHOOL PROFILE (tenant-scoped)
    // GET /api/v1/schools/me
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/schools/me")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','VICE_PRINCIPAL')")
    @Cacheable(value = CacheConfig.CACHE_SCHOOL,
               key = "T(com.vidyaos.tenant.TenantContext).getCurrentTenant().toString()")
    @Operation(summary = "Get current school profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySchool() {
        UUID schoolId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ApiResponse.ok(queryService.getSchoolProfile(schoolId)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUPER ADMIN — list all schools
    // GET /api/v1/schools?state=Karnataka&type=PRIVATE&plan=GROWTH&page=0&size=20
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/schools")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all schools — Super Admin",
               description = "Paginated list with optional filters: state, type, plan")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> listSchools(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String plan,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            queryService.listSchools(state, type, plan, page, size)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHAIN MANAGEMENT
    // POST /api/v1/chains
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/chains")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new school chain",
               description = "Creates chain parent record and a Chain Admin user. SUPER_ADMIN only.")
    public ResponseEntity<ApiResponse<ChainCreateResponse>> createChain(
            @Valid @RequestBody ChainCreateRequest request) {
        ChainCreateResponse response = onboardingService.createChain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Chain created successfully", response));
    }

    // ══════════════════════════════════════════════════════════════════════
    // BRANCH ONBOARDING under a chain
    // POST /api/v1/chains/{chainId}/branches
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/chains/{chainId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CHAIN_ADMIN')")
    @Operation(summary = "Onboard a new branch under an existing chain",
               description = "Creates branch school with unique branch_code within the chain. SUPER_ADMIN or CHAIN_ADMIN only.")
    public ResponseEntity<ApiResponse<BranchOnboardResponse>> onboardBranch(
            @PathVariable UUID chainId,
            @Valid @RequestBody BranchOnboardRequest request,
            @AuthenticationPrincipal VidyaUserDetails userDetails) {

        // Chain Admin can only add branches to their own chain
        if (userDetails.isChainAdmin() && !chainId.equals(userDetails.getChainId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(
                    "You can only manage branches within your own chain", "CHAIN_ACCESS_DENIED"));
        }

        BranchOnboardResponse response = onboardingService.onboardBranch(chainId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Branch onboarded successfully", response));
    }

    // ══════════════════════════════════════════════════════════════════════
    // LIST BRANCHES of a chain
    // GET /api/v1/chains/{chainId}/branches
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chains/{chainId}/branches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CHAIN_ADMIN')")
    @Cacheable(value = CacheConfig.CACHE_CHAIN_BRANCHES, key = "#chainId.toString()")
    @Operation(summary = "List all branches of a chain")
    public ResponseEntity<ApiResponse<Object>> listBranches(
            @PathVariable UUID chainId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal VidyaUserDetails userDetails) {

        if (userDetails.isChainAdmin() && !chainId.equals(userDetails.getChainId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("Access denied to this chain", "CHAIN_ACCESS_DENIED"));
        }

        return ResponseEntity.ok(ApiResponse.ok(
            queryService.listBranches(chainId, page, size)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET CHAIN PROFILE
    // GET /api/v1/chains/{chainId}
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chains/{chainId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CHAIN_ADMIN')")
    @Cacheable(value = CacheConfig.CACHE_CHAIN, key = "#chainId.toString()")
    @Operation(summary = "Get chain profile with branch summary")
    public ResponseEntity<ApiResponse<Object>> getChain(
            @PathVariable UUID chainId,
            @AuthenticationPrincipal VidyaUserDetails userDetails) {

        if (userDetails.isChainAdmin() && !chainId.equals(userDetails.getChainId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("Access denied to this chain", "CHAIN_ACCESS_DENIED"));
        }

        return ResponseEntity.ok(ApiResponse.ok(queryService.getChainProfile(chainId)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // DEACTIVATE a school
    // PATCH /api/v1/schools/{schoolId}/deactivate
    // ══════════════════════════════════════════════════════════════════════

    @PatchMapping("/schools/{schoolId}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @CacheEvict(value = CacheConfig.CACHE_SCHOOL, key = "#schoolId.toString()")
    @Operation(summary = "Deactivate a school")
    public ResponseEntity<ApiResponse<Void>> deactivateSchool(@PathVariable UUID schoolId) {
        queryService.deactivateSchool(schoolId);
        return ResponseEntity.ok(ApiResponse.ok("School deactivated"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHANGE PLAN
    // PATCH /api/v1/schools/{schoolId}/plan
    // ══════════════════════════════════════════════════════════════════════

    @PatchMapping("/schools/{schoolId}/plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @CacheEvict(value = CacheConfig.CACHE_SCHOOL, key = "#schoolId.toString()")
    @Operation(summary = "Change the subscription plan of a school")
    public ResponseEntity<ApiResponse<Void>> changePlan(
            @PathVariable UUID schoolId,
            @RequestParam String plan) {
        queryService.changePlan(schoolId, plan);
        return ResponseEntity.ok(ApiResponse.ok("Plan updated to " + plan));
    }
}
