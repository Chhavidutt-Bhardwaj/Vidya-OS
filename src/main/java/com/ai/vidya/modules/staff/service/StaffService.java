package com.ai.vidya.modules.staff.service;

import com.ai.vidya.common.response.ApiResponse;
import com.ai.vidya.config.CacheConfig;
import com.ai.vidya.config.CacheKeyHelper;
import com.ai.vidya.modules.staff.dto.request.CreateStaffRequest;
import com.ai.vidya.modules.staff.dto.request.UpdateStaffRequest;
import com.ai.vidya.modules.staff.dto.response.StaffResponse;
import com.ai.vidya.modules.staff.entity.*;
import com.ai.vidya.modules.staff.repository.StaffRepository;
import com.ai.vidya.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Core service for Staff CRUD operations.
 *
 * <h3>Caching strategy</h3>
 * <ul>
 *   <li>{@code staffDetail}  – single staff DTO, keyed by tenantId:schoolId:staffId.</li>
 *   <li>{@code staffList}    – paginated list, keyed by tenantId:schoolId:page:size:role.</li>
 *   <li>Creates and updates use {@code @CachePut} to refresh the detail cache in-place.</li>
 *   <li>Deletes evict the detail entry AND all list variants for that tenant+school.</li>
 * </ul>
 *
 * <p><b>Cache key safety:</b> All keys are built through {@link CacheKeyHelper}
 * which embeds tenantId and schoolId — no cross-tenant data leakage possible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    // ── CREATE ────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = CacheConfig.STAFF_LIST, allEntries = true) // wipe list cache for this tenant
    public StaffResponse createStaff(CreateStaffRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        if (staffRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new IllegalArgumentException("A staff member with this email already exists: " + request.getEmail());
        }

        Staff staff = staffMapper.toEntity(request);
        staff.setTenantId(tenantId);
        staff.setSchoolId(schoolId);

        Staff saved = staffRepository.save(staff);
        log.info("Staff created: id={} role={} tenant={} school={}", saved.getId(), saved.getRoleType(), tenantId, schoolId);

        return staffMapper.toResponse(saved);
    }

    // ── READ (single) ─────────────────────────────────────────────────────

    /**
     * Fetch a single staff member. Result is cached in {@code staffDetail}.
     *
     * <p>SpEL key delegates to {@link CacheKeyHelper#staffDetail(UUID)} which
     * reads TenantContext — safe as context is set before this method is called.
     */
    @Cacheable(
        value  = CacheConfig.STAFF_DETAIL,
        key    = "T(com.ai.vidya.config.CacheKeyHelper).staffDetail(#id)",
        unless = "#result == null"
    )
    public StaffResponse getStaffById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        return staffRepository
                .findByIdAndTenantIdAndSchoolId(id, tenantId, schoolId)
                .map(staffMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
    }

    // ── READ (paginated list) ─────────────────────────────────────────────

    @Cacheable(
        value  = CacheConfig.STAFF_LIST,
        key    = "T(com.ai.vidya.config.CacheKeyHelper).staffList(#page, #size, #roleType != null ? #roleType.name() : 'ALL')",
        unless = "#result == null"
    )
    public Page<StaffResponse> getAllStaff(int page, int size, StaffRoleType roleType) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Page<Staff> staffPage = (roleType != null)
                ? staffRepository.findAllByTenantIdAndSchoolIdAndRoleType(tenantId, schoolId, roleType, pageable)
                : staffRepository.findAllByTenantIdAndSchoolId(tenantId, schoolId, pageable);

        return staffPage.map(staffMapper::toResponse);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    @Transactional
    @Caching(
        put    = @CachePut(value = CacheConfig.STAFF_DETAIL,
                           key = "T(com.ai.vidya.config.CacheKeyHelper).staffDetail(#id)"),
        evict  = @CacheEvict(value = CacheConfig.STAFF_LIST, allEntries = true)
    )
    public StaffResponse updateStaff(UUID id, UpdateStaffRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        Staff staff = staffRepository
                .findByIdAndTenantIdAndSchoolId(id, tenantId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));

        applyUpdates(staff, request);
        Staff updated = staffRepository.save(staff);
        log.info("Staff updated: id={} tenant={}", id, tenantId);

        return staffMapper.toResponse(updated);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.STAFF_DETAIL,
                    key = "T(com.ai.vidya.config.CacheKeyHelper).staffDetail(#id)"),
        @CacheEvict(value = CacheConfig.STAFF_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.STAFF_PERFORMANCE,
                    key = "T(com.ai.vidya.config.CacheKeyHelper).staffPerformance(#id)"),
        @CacheEvict(value = CacheConfig.AI_INSIGHTS,
                    key = "T(com.ai.vidya.config.CacheKeyHelper).aiInsights(#id)")
    })
    public void deleteStaff(UUID id, String deletedBy) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID schoolId = TenantContext.requireSchoolId();

        Staff staff = staffRepository
                .findByIdAndTenantIdAndSchoolId(id, tenantId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));

        staff.softDelete(deletedBy);
        staffRepository.save(staff);
        log.info("Staff soft-deleted: id={} by={}", id, deletedBy);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Applies update request fields using Java 21 pattern matching for concise
     * subtype-aware field mapping.
     */
    private void applyUpdates(Staff staff, UpdateStaffRequest req) {
        if (req.getName()       != null) staff.setName(req.getName());
        if (req.getPhone()      != null) staff.setPhone(req.getPhone());
        if (req.getDepartment() != null) staff.setDepartment(req.getDepartment());
        if (req.getSalary()     != null) staff.setSalary(req.getSalary());
        if (req.getStatus()     != null) staff.setStatus(req.getStatus());

        switch (staff) {
            case Teacher t -> {
                if (req.getSubject()         != null) t.setSubject(req.getSubject());
                if (req.getExperienceYears() != null) t.setExperienceYears(req.getExperienceYears());
                if (req.getQualification()   != null) t.setQualification(req.getQualification());
            }
            case Principal p -> {
                if (req.getLeadershipScore()          != null) p.setLeadershipScore(req.getLeadershipScore());
                if (req.getSchoolPerformanceRating()  != null) p.setSchoolPerformanceRating(req.getSchoolPerformanceRating());
            }
            case Accountant a -> {
                if (req.getFinanceAccessLevel() != null) a.setFinanceAccessLevel(req.getFinanceAccessLevel());
                if (req.getManagedBudget()      != null) a.setManagedBudget(req.getManagedBudget());
            }
            case ExamCoordinator e -> {
                if (req.getSchedulingEfficiency() != null) e.setSchedulingEfficiency(req.getSchedulingEfficiency());
                if (req.getExamPlanningScore()    != null) e.setExamPlanningScore(req.getExamPlanningScore());
            }
            default -> { /* HR — no extra fields */ }
        }
    }
}
