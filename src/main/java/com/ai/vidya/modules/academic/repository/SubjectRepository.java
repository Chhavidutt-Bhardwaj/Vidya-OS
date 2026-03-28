package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.common.enums.SubjectType;
import com.ai.vidya.modules.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    @Query("SELECT s FROM Subject s WHERE s.id = :id AND s.deleted = false")
    Optional<Subject> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT s FROM Subject s
        WHERE s.schoolId = :schoolId AND s.deleted = false
        ORDER BY s.subjectType, s.name
        """)
    List<Subject> findAllBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("""
        SELECT s FROM Subject s
        WHERE s.schoolId = :schoolId
          AND s.active = true
          AND s.deleted = false
        ORDER BY s.subjectType, s.name
        """)
    List<Subject> findAllActiveBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("""
        SELECT s FROM Subject s
        WHERE s.schoolId = :schoolId
          AND s.subjectType = :type
          AND s.deleted = false
        ORDER BY s.name
        """)
    List<Subject> findAllBySchoolIdAndType(
        @Param("schoolId") UUID schoolId,
        @Param("type") SubjectType type
    );

    @Query("""
        SELECT s FROM Subject s
        WHERE s.schoolId = :schoolId
          AND s.code = :code
          AND s.deleted = false
        """)
    Optional<Subject> findBySchoolIdAndCode(
        @Param("schoolId") UUID schoolId,
        @Param("code")     String code
    );

    @Query("""
        SELECT COUNT(s) > 0 FROM Subject s
        WHERE s.schoolId = :schoolId
          AND s.code = :code
          AND s.deleted = false
        """)
    boolean existsBySchoolIdAndCode(
        @Param("schoolId") UUID schoolId,
        @Param("code")     String code
    );
}
