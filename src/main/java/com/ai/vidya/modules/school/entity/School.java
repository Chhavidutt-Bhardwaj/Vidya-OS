package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.BoardType;
import com.ai.vidya.common.enums.OnboardingStep;
import com.ai.vidya.common.enums.PlanType;
import com.ai.vidya.common.enums.SchoolType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Core school entity. Each row = one physical campus / branch.
 *
 * Standalone school  → chain == null
 * Chain branch       → chain != null, branchCode set
 * Main/HQ branch     → chain != null, isHeadquarter == true
 */
@Entity
@Table(
    name = "schools",
    indexes = {
        @Index(name = "idx_school_chain_id",   columnList = "chain_id"),
        @Index(name = "idx_school_active",      columnList = "active"),
        @Index(name = "idx_school_type",        columnList = "type"),
        @Index(name = "idx_school_plan",        columnList = "plan"),
        @Index(name = "idx_school_board",       columnList = "board"),
        @Index(name = "idx_school_udise",       columnList = "udise_code"),
        @Index(name = "idx_school_onboard_step",columnList = "onboarding_step")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School extends BaseEntity {

    // ── Chain / Branch relationship ────────────────────────────────────────

    /**
     * NULL = standalone school (no chain)
     * SET  = this school is a branch of the chain
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_id")
    private SchoolChain chain;

    /** e.g. "BRANCH-001", "NORTH-CAMPUS" — unique within a chain */
    @Column(name = "branch_code", length = 30)
    private String branchCode;

    /** Human-readable branch label: "Main Branch", "Sector 62 Campus" */
    @Column(name = "branch_name", length = 100)
    private String branchName;

    /**
     * Marks the headquarter / admin branch of a chain.
     * Only one branch per chain should have this flag = true.
     */
    @Column(name = "is_headquarter", nullable = false)
    @Builder.Default
    private boolean headquarter = false;

    // ── Basic Info ─────────────────────────────────────────────────────────

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SchoolType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BoardType board;

    /** Teaching medium: english, hindi, tamil, kannada, telugu, marathi, bengali */
    @Column(length = 30)
    private String medium;

    /** Government UDISE+ code — unique per campus */
    @Column(name = "udise_code", length = 20, unique = true)
    private String udiseCode;

    /** CBSE/ICSE affiliation number */
    @Column(name = "affiliation_no", length = 50)
    private String affiliationNo;

    /** Old DISE code if migrated */
    @Column(name = "dise_code", length = 20)
    private String diseCode;

    // ── Plan & Status ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PlanType plan = PlanType.STARTER;

    @Column(name = "student_count", nullable = false)
    @Builder.Default
    private int studentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Onboarding ─────────────────────────────────────────────────────────

    /**
     * Tracks which step of onboarding the school completed.
     * BASIC_INFO → CONTACT → ADDRESS → ACADEMIC → DOCUMENTS → COMPLETE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_step", length = 30, nullable = false)
    @Builder.Default
    private OnboardingStep onboardingStep = OnboardingStep.BASIC_INFO;

    @Column(name = "onboarding_complete", nullable = false)
    @Builder.Default
    private boolean onboardingComplete = false;

    // ── Associations ───────────────────────────────────────────────────────

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private SchoolAddress address;

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private SchoolSettings settings;

    @OneToOne(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private SchoolBasicInfo basicInfo;

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AcademicYear> academicYears = new ArrayList<>();

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolGradeRange> gradeRanges = new ArrayList<>();

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolFacility> facilities = new ArrayList<>();

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolSocialLink> socialLinks = new ArrayList<>();

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean isChainBranch() {
        return chain != null;
    }

    public boolean isStandalone() {
        return chain == null;
    }

    public String getDisplayName() {
        if (branchName != null && !branchName.isBlank()) {
            return name + " — " + branchName;
        }
        return name;
    }

    public void advanceOnboarding(OnboardingStep next) {
        this.onboardingStep = next;
        if (next == OnboardingStep.COMPLETE) {
            this.onboardingComplete = true;
        }
    }
}