package com.ai.vidya.modules.academic.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.modules.school.entity.AcademicYear;
import com.ai.vidya.modules.school.entity.School;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * One grade/class offered by a school in a specific academic year.
 * e.g. "Class 10" in "2024-25", "Nursery" in "2024-25"
 *
 * A class can have multiple sections (A, B, C).
 * Subjects are assigned at the class level via ClassSubject,
 * then section-level teacher assignments happen via SectionSubjectTeacher.
 *
 * Hierarchy:
 *   School → AcademicYear → SchoolClass → SchoolSection
 *                                       → ClassSubject (subjects taught)
 */
@Entity
@Table(
    name = "school_classes",
    indexes = {
        @Index(name = "idx_class_school_id",   columnList = "school_id"),
        @Index(name = "idx_class_year_id",     columnList = "academic_year_id"),
        @Index(name = "idx_class_grade_order", columnList = "grade_order")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_class_year_name",
            columnNames = {"academic_year_id", "name"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /**
     * Grade/class name as displayed.
     * e.g. "Nursery", "KG1", "KG2", "Class 1" … "Class 12"
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Short alias used on timetables and reports.
     * e.g. "I", "II", "X", "XII", "Nur"
     */
    @Column(name = "display_name", length = 30)
    private String displayName;

    /**
     * Numeric order for sorting classes correctly.
     * Nursery=0, KG1=1, KG2=2, Class1=3 … Class12=14
     */
    @Column(name = "grade_order", nullable = false)
    private int gradeOrder;

    /**
     * Room/hall label if all sections of this class share one space
     * (less common — usually set per section).
     */
    @Column(name = "room", length = 30)
    private String room;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Associations ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchoolSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClassSubject> classSubjects = new ArrayList<>();

    // ── Helper ──────────────────────────────────────────────────────────────

    public int getSectionCount() {
        return (int) sections.stream().filter(s -> !s.getDeleted()).count();
    }
}
