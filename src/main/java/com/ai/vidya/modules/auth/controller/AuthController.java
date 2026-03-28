package com.ai.vidya.modules.auth.controller;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.modules.auth.dto.LoginRequest;
import com.ai.vidya.modules.auth.dto.LoginResponse;
import com.ai.vidya.modules.auth.dto.TokenRefreshRequest;
import com.ai.vidya.modules.auth.dto.TokenRefreshResponse;
import com.ai.vidya.modules.auth.service.AuthService;
import com.ai.vidya.security.VidyaUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
            ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(
            ApiResponse.ok(authService.refreshToken(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody TokenRefreshRequest request,
            @AuthenticationPrincipal VidyaUserDetails userDetails) {
        authService.logout(request.getRefreshToken(), userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile from JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> me(
            @AuthenticationPrincipal VidyaUserDetails userDetails) {
        // Re-use login to build full profile — or build a lighter ProfileResponse if needed
        return ResponseEntity.ok(ApiResponse.ok(
            LoginResponse.builder()
                .userId(userDetails.getUserId())
                .userType(userDetails.getAuthorities().toString())
                .schoolId(userDetails.getSchoolId())
                .chainId(userDetails.getChainId())
                .build()));
    }
}
