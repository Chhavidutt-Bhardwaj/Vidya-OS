package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.SectionSubjectTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionSubjectTeacherRepository extends JpaRepository<SectionSubjectTeacher, UUID> {

    @Query("SELECT t FROM SectionSubjectTeacher t WHERE t.id = :id AND t.deleted = false")
    Optional<SectionSubjectTeacher> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT t FROM SectionSubjectTeacher t
        JOIN FETCH t.classSubject cs
        JOIN FETCH cs.subject
        WHERE t.section.id = :sectionId
          AND t.deleted = false
        """)
    List<SectionSubjectTeacher> findAllBySectionId(@Param("sectionId") UUID sectionId);

    /** All sections and subjects a teacher is assigned to in an academic year. */
    @Query("""
        SELECT t FROM SectionSubjectTeacher t
        JOIN FETCH t.section sec
        JOIN FETCH sec.schoolClass sc
        JOIN FETCH t.classSubject cs
        JOIN FETCH cs.subject
        WHERE t.teacherId = :teacherId
          AND sc.academicYear.id = :yearId
          AND t.deleted = false
        """)
    List<SectionSubjectTeacher> findAllByTeacherAndYear(
        @Param("teacherId") UUID teacherId,
        @Param("yearId")    UUID yearId
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM SectionSubjectTeacher t
        WHERE t.section.id = :sectionId
          AND t.classSubject.id = :classSubjectId
          AND t.assignmentType = com.ai.vidya.modules.academic.entity.SectionSubjectTeacher.AssignmentType.THEORY
          AND t.deleted = false
        """)
    boolean hasTheoryTeacherForSectionSubject(
        @Param("sectionId")      UUID sectionId,
        @Param("classSubjectId") UUID classSubjectId
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM SectionSubjectTeacher t
        WHERE t.section.id = :sectionId
          AND t.classSubject.id = :classSubjectId
          AND t.deleted = false
        """)
    boolean existsBySectionAndClassSubject(
        @Param("sectionId")      UUID sectionId,
        @Param("classSubjectId") UUID classSubjectId
    );
}
