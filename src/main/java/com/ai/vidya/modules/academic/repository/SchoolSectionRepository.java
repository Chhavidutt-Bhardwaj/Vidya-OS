package com.ai.vidya.modules.academic.repository;

import com.ai.vidya.modules.academic.entity.SchoolSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolSectionRepository extends JpaRepository<SchoolSection, UUID> {

    @Query("SELECT s FROM SchoolSection s WHERE s.id = :id AND s.deleted = false")
    Optional<SchoolSection> findByIdNotDeleted(@Param("id") UUID id);

    @Query("""
        SELECT s FROM SchoolSection s
        WHERE s.schoolClass.id = :classId
          AND s.deleted = false
        ORDER BY s.name ASC
        """)
    List<SchoolSection> findAllByClassId(@Param("classId") UUID classId);

    @Query("""
        SELECT COUNT(s) > 0 FROM SchoolSection s
        WHERE s.schoolClass.id = :classId
          AND s.name = :name
          AND s.deleted = false
        """)
    boolean existsByClassIdAndName(
        @Param("classId") UUID classId,
        @Param("name")    String name
    );

    /** All sections taught by a specific teacher across a school in an academic year. */
    @Query("""
        SELECT s FROM SchoolSection s
        WHERE s.classTeacherId = :teacherId
          AND s.schoolClass.academicYear.id = :yearId
          AND s.deleted = false
        """)
    List<SchoolSection> findAllByClassTeacherAndYear(
        @Param("teacherId") UUID teacherId,
        @Param("yearId")    UUID yearId
    );

    @Modifying
    @Query("UPDATE SchoolSection s SET s.studentCount = s.studentCount + 1 WHERE s.id = :id")
    void incrementStudentCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE SchoolSection s SET s.studentCount = s.studentCount - 1 WHERE s.id = :id AND s.studentCount > 0")
    void decrementStudentCount(@Param("id") UUID id);
}
