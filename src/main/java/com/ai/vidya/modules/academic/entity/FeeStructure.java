package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.AcademicYear;
import com.ai.vidya.modules.school.entity.SchoolGradeRange;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level fee structure for one academic year and one grade range.
 *
 * <p>A school typically has one FeeStructure per grade range per year.
 * e.g. "2025-26 / Primary (1–5)" and "2025-26 / Secondary (9–10)" are
 * two separate FeeStructure rows with their own FeeStructureHead children.
 *
 * <p>Rollover (RolloverTemplate.cloneFeeStructure = true):
 * A new FeeStructure is created for the new year; all FeeStructureHead
 * children are cloned with amounts copied verbatim. The admin can then
 * update amounts before activating the year.
 */
@Entity
@Table(
    name = "fee_structures",
    indexes = {
        @Index(name = "idx_fs_academic_year_id",  columnList = "academic_year_id"),
        @Index(name = "idx_fs_grade_range_id",    columnList = "grade_range_id"),
        @Index(name = "idx_fs_active",            columnList = "active")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_fs_year_grade",
            columnNames = {"academic_year_id", "grade_range_id"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /**
     * The grade segment this structure applies to.
     * NULL = applies to all grades (used for school-wide one-time fees
     * like registration that don't differ by grade).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_range_id")
    private SchoolGradeRange gradeRange;

    /** Human-readable label e.g. "Primary Fee 2025-26" */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Whether this structure is currently active and should be used
     * when computing student fee invoices. Set to false for draft structures
     * under review before the year is activated.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = false;

    /** Cloned from previous year's structure during rollover */
    @Column(name = "is_rolled_over", nullable = false)
    @Builder.Default
    private boolean rolledOver = false;

    /** Optional admin notes / revision history */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "feeStructure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FeeStructureHead> heads = new ArrayList<>();
}