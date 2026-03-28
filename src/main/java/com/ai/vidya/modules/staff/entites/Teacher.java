package com.ai.vidya.modules.staff.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Teacher — extends {@link Staff} via JOINED inheritance.
 * Only teacher-specific columns live in the {@code teachers} table.
 */
@Entity
@Table(name = "teachers")
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher extends Staff {

    @Column(length = 100)
    private String subject;

    /** Years of teaching experience. */
    @Column(name = "experience_years")
    private Integer experienceYears;

    /** Highest academic qualification, e.g. B.Ed, M.Sc. */
    @Column(length = 150)
    private String qualification;

    /**
     * Composite score (0–100) computed from attendance %, student results %,
     * and feedback ratings. Refreshed by {@link com.ai.vidya.modules.staff.service.PerformanceService}.
     */
    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;
}
