package com.ai.vidya.modules.user.repository;

import com.ai.vidya.modules.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * All methods use explicit @Query to avoid Spring Data derived-query
 * tokenizer failures on compound field names.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    @Query("""
        SELECT r FROM Role r
        LEFT JOIN FETCH r.permissions
        WHERE r.name = :name
          AND r.systemRole = true
        """)
    Optional<Role> findSystemRoleByName(@Param("name") String name);

    /**
     * Finds any role by name — system or custom.
     * Used by PlatformAdminSeeder to look up roles by the configured name.
     */
    @Query("SELECT r FROM Role r WHERE r.name = :name AND r.deleted = false")
    Optional<Role> findByName(@Param("name") String name);

    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.name = :name")
    boolean existsByName(@Param("name") String name);
}