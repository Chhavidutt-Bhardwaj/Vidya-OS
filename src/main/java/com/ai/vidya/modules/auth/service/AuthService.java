package com.ai.vidya.modules.auth.service;

import com.ai.vidya.exception.AuthException;
import com.ai.vidya.modules.auth.dto.LoginRequest;
import com.ai.vidya.modules.auth.dto.LoginResponse;
import com.ai.vidya.modules.auth.dto.TokenRefreshResponse;
import com.ai.vidya.modules.school.entity.SchoolChain;
import com.ai.vidya.modules.school.repository.SchoolChainRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import com.ai.vidya.modules.user.entity.Permission;
import com.ai.vidya.modules.user.entity.Role;
import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import com.ai.vidya.security.JwtService;
import com.ai.vidya.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ai.vidya.config.CacheConfig.CACHE_USER_DETAILS;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS  = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final SystemUserRepository userRepo;
    private final SchoolRepository schoolRepo;
    private final SchoolChainRepository chainRepo;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // ── Login ─────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {

        SystemUser user = userRepo.findByEmailAndDeletedFalse(request.getEmail())
            .orElseThrow(() -> new AuthException("Invalid email or password"));

        // Check account lock
        if (user.isLocked()) {
            throw new AuthException(String.format(
                "Account locked due to too many failed attempts. Try again after %s.",
                user.getLockedUntil().toString()));
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new AuthException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AuthException("Your account is disabled. Please contact your school administrator.");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        // Initialise lazy collections for JWT claims + response
        user.getRoles().forEach(r -> r.getPermissions().size());

        // School / chain name enrichment
        String schoolName = null;
        String chainName  = null;

        if (user.getSchoolId() != null) {
            schoolName = schoolRepo.findNameById(user.getSchoolId()).orElse(null);
        }
        if (user.getChainId() != null) {
            chainName = chainRepo.findById(user.getChainId())
                .map(SchoolChain::getName).orElse(null);
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());

        log.info("Login success — user={} school={} chain={}",
            user.getId(), user.getSchoolId(), user.getChainId());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getAccessExpirySeconds())
            .userId(user.getId())
            .userType(user.getUserType().name())
            .schoolId(user.getSchoolId())
            .schoolName(schoolName)
            .chainId(user.getChainId())
            .chainName(chainName)
            .fullName(user.getFullName())
            .roles(roles)
            .permissions(permissions)
            .build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────

    @Transactional
    public TokenRefreshResponse refreshToken(String rawRefreshToken) {
        UUID userId = refreshTokenService.getUserIdFromRawToken(rawRefreshToken);

        SystemUser user = userRepo.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        if (!user.isActive()) {
            throw new AuthException("Account is disabled");
        }

        user.getRoles().forEach(r -> r.getPermissions().size());

        // Rotate refresh token (old token revoked, new issued)
        String newRefreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        String newAccessToken  = jwtService.generateAccessToken(user);

        return TokenRefreshResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getAccessExpirySeconds())
            .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = CACHE_USER_DETAILS, key = "#userId.toString()")
    public void logout(String rawRefreshToken, UUID userId) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
        log.info("Logout — userId={}", userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void handleFailedAttempt(SystemUser user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked — userId={} email={}", user.getId(), user.getEmail());
        }
        userRepo.save(user);
    }
}
