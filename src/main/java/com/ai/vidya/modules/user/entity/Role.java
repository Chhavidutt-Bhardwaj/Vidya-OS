package com.ai.vidya.modules.user.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "roles",
    indexes = {
        @Index(name = "idx_role_name",     columnList = "name", unique = true),
        @Index(name = "idx_role_school_id",columnList = "school_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    /**
     * System roles: SUPER_ADMIN, CHAIN_ADMIN, SCHOOL_ADMIN, PRINCIPAL, TEACHER, PARENT etc.
     * School-specific custom roles can also be created per school.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /** NULL = global system role; SET = school-specific custom role */
    @Column(name = "school_id")
    private UUID schoolId;

    /** NULL = not chain-scoped; SET = chain-level role (e.g. CHAIN_ADMIN) */
    @Column(name = "chain_id")
    private UUID chainId;

    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private boolean systemRole = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
