package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An academic year for a school — e.g. "2024-25".
 * Drives timetable, fee, exam, and report structures.
 *
 * Many-to-one with School. One school has many academic years,
 * but only one is current/active at a time.
 */
@Entity
@Table(
    name = "academic_years",
    indexes = {
        @Index(name = "idx_ay_school_id",  columnList = "school_id"),
        @Index(name = "idx_ay_current",    columnList = "is_current"),
        @Index(name = "idx_ay_start_date", columnList = "start_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name  = "uq_ay_school_label",
            columnNames = {"school_id", "label"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /** Display label e.g. "2024-25" */
    @Column(nullable = false, length = 20)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Only one AcademicYear per school can be current.
     * The service layer must enforce this constraint.
     */
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = false;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolTerm> terms = new ArrayList<>();

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolShift> shifts = new ArrayList<>();
}