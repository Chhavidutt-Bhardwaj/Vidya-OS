package com.ai.vidya.modules.user.repository;

import com.ai.vidya.modules.user.entity.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {

    Optional<SystemUser> findByEmailAndDeletedFalse(String email);

    Optional<SystemUser> findByIdAndDeletedFalse(UUID id);

    boolean existsByEmailAndDeletedFalse(String email);

    List<SystemUser> findAllBySchoolIdAndDeletedFalse(UUID schoolId);

    @Query("""
        SELECT u FROM SystemUser u
        JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.email = :email AND u.deleted = false
        """)
    Optional<SystemUser> findByEmailWithRolesAndPermissions(@Param("email") String email);

    @Query("""
        SELECT u FROM SystemUser u
        JOIN FETCH u.roles r
        LEFT JOIN FETCH r.permissions
        WHERE u.id = :id AND u.deleted = false
        """)
    Optional<SystemUser> findByIdWithRolesAndPermissions(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE SystemUser u SET u.active = false WHERE u.schoolId = :schoolId")
    void deactivateAllBySchoolId(@Param("schoolId") UUID schoolId);
}