package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group/chain of schools (e.g., "DPS Group", "Ryan International").
 * A chain can own multiple School branches.
 *
 * Computed @Formula fields (totalBranches, activeBranches, totalStudents) register
 * aggregate values as first-class Hibernate mapped attributes so JPQL queries can
 * reference them in SELECT / WHERE / ORDER BY without PathElementException.
 */
@Entity
@Table(
        name = "school_chains",
        indexes = {
                @Index(name = "idx_chain_active", columnList = "active")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolChain extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    /** Short code like "DPS", "RYAN" — unique across chains */
    @Column(name = "chain_code", nullable = false, length = 30, unique = true)
    private String chainCode;

    @Column(length = 1000)
    private String description;

    /** Official website of the chain */
    @Column(length = 255)
    private String website;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Computed / formula fields ──────────────────────────────────────────
    //
    // WHY @Formula instead of helper methods or SIZE(branches) in JPQL:
    //   - Plain Java methods (getBranchCount) are invisible to Hibernate's metamodel.
    //     Any JPQL query that references them throws PathElementException at startup.
    //   - SIZE(c.branches) in JPQL forces a JOIN which inflates result sets.
    //   - @Formula injects a correlated SQL subquery per row — no extra JOIN,
    //     and the field is a real mapped attribute resolvable by JPQL.
    //   - All subqueries filter is_deleted = false to honour soft-delete.
    //   - COALESCE guards SUM against NULL when a chain has zero branches.

    /** Total non-deleted branches under this chain. */
    @Formula("(SELECT COUNT(s.id) FROM schools s" +
            " WHERE s.chain_id = id" +
            "   AND s.is_deleted = false)")
    private int totalBranches;

    /** Non-deleted AND active branches — useful for dashboard KPIs. */
    @Formula("(SELECT COUNT(s.id) FROM schools s" +
            " WHERE s.chain_id = id" +
            "   AND s.is_deleted = false" +
            "   AND s.active = true)")
    private int activeBranches;

    /**
     * Sum of student_count across all non-deleted branches.
     * student_count is maintained on the School row (updated on admission/exit).
     */
    @Formula("(SELECT COALESCE(SUM(s.student_count), 0) FROM schools s" +
            " WHERE s.chain_id = id" +
            "   AND s.is_deleted = false)")
    private int totalStudents;

    // ── Association ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "chain", fetch = FetchType.LAZY)
    @Builder.Default
    private List<School> branches = new ArrayList<>();

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Backward-compatible alias — delegates to the @Formula field.
     * Prefer totalBranches directly in new code.
     */
    public int getBranchCount() {
        return totalBranches;
    }
}