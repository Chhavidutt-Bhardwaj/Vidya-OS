package com.ai.vidya.modules.school.repository;

import com.ai.vidya.modules.school.entity.SchoolChain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolChainRepository extends JpaRepository<SchoolChain, UUID> {

    Page<SchoolChain> findByDeletedFalseOrderByNameAsc(Pageable pageable);

    @Modifying
    @Query("UPDATE SchoolChain c SET c.totalBranches = c.totalBranches + 1 WHERE c.id = :id")
    void incrementBranchCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE SchoolChain c SET c.totalStudents = c.totalStudents + :count WHERE c.id = :id")
    void updateStudentCount(@Param("id") UUID id, @Param("count") int count);

    boolean existsByChainCodeAndDeletedFalse(@NotBlank(message = "Chain code is required") @Size(max = 30) @Pattern(regexp = "^[A-Z0-9_-]{2,30}$",
             message = "Chain code must be 2–30 uppercase alphanumeric characters") String chainCode);

    @Query("SELECT COUNT(c) > 0 FROM SchoolChain c WHERE c.chainCode = :chainCode AND c.deleted = false")
    boolean existsByChainCode(@Param("chainCode") String chainCode);

    @Query("SELECT c FROM SchoolChain c WHERE c.id = :id AND c.deleted = false")
    Optional<SchoolChain> findByIdAndDeletedFalse(@Param("id") UUID id);

    @Query("SELECT c FROM SchoolChain c WHERE c.chainCode = :chainCode AND c.deleted = false")
    Optional<SchoolChain> findByChainCode(@Param("chainCode") String chainCode);

}
