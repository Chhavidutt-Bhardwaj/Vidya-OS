package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {

    @Query("SELECT cs FROM ClassSubject cs WHERE cs.id = :id AND cs.deleted = false")
    Optional<ClassSubject> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT cs FROM ClassSubject cs
        JOIN FETCH cs.subject
        WHERE cs.schoolClass.id = :classId
          AND cs.deleted = false
        ORDER BY cs.subject.subjectType, cs.subject.name
        """)
    List<ClassSubject> findAllByClassId(@Param("classId") UUID classId);

    @Query("""
        SELECT COUNT(cs) > 0 FROM ClassSubject cs
        WHERE cs.schoolClass.id = :classId
          AND cs.subject.id = :subjectId
          AND cs.deleted = false
        """)
    boolean existsByClassIdAndSubjectId(
        @Param("classId")   UUID classId,
        @Param("subjectId") UUID subjectId
    );

    @Query("""
        SELECT cs FROM ClassSubject cs
        JOIN FETCH cs.subject
        WHERE cs.schoolClass.id = :classId
          AND cs.subject.id = :subjectId
          AND cs.deleted = false
        """)
    Optional<ClassSubject> findByClassIdAndSubjectId(
        @Param("classId")   UUID classId,
        @Param("subjectId") UUID subjectId
    );
}
