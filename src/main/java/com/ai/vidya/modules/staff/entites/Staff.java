package com.ai.vidya.modules.staff.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Base staff entity.
 *
 * <p>Inheritance strategy: {@link InheritanceType#JOINED} — each subtype gets
 * its own table. The discriminator column {@code role_type} on this table
 * identifies the concrete type.
 *
 * <p>{@code @SQLRestriction} ensures every JPQL / Criteria query automatically
 * adds {@code is_deleted = false} — soft-delete is transparent.
 *
 * <p>Composite index on {@code (tenant_id, school_id)} is mandatory for
 * multi-tenant query performance.
 */
@Entity
@Table(
    name = "staff",
    indexes = {
        @Index(name = "idx_staff_tenant_school", columnList = "tenant_id, school_id"),
        @Index(name = "idx_staff_role_type",     columnList = "role_type"),
        @Index(name = "idx_staff_email",         columnList = "email", unique = true),
        @Index(name = "idx_staff_status",        columnList = "status"),
        @Index(name = "idx_staff_department",    columnList = "department")
    }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role_type", discriminatorType = DiscriminatorType.STRING)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Staff extends BaseEntity {

    // ── Identity ──────────────────────────────────────────────────────────

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    // ── Classification ────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", insertable = false, updatable = false, length = 30)
    private StaffRoleType roleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Department department;

    // ── HR data ───────────────────────────────────────────────────────────

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    // ── Nested enums ──────────────────────────────────────────────────────

    public enum Department {
        ACADEMIC, FINANCE, ADMIN
    }

    public enum StaffStatus {
        ACTIVE, INACTIVE, ON_LEAVE
    }
}
