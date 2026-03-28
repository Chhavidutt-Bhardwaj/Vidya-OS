package com.ai.vidya.modules.exam.entity;

import com.ai.vidya.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "exam_results",
    indexes = {
        @Index(name = "idx_er_exam_schedule_id", columnList = "exam_schedule_id"),
        @Index(name = "idx_er_student_id",       columnList = "student_id, academic_year_id"),
        @Index(name = "idx_er_section_id",       columnList = "section_id"),
        @Index(name = "idx_er_published",        columnList = "is_published"),
        @Index(name = "idx_er_subject",          columnList = "school_id, subject_code")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_result",
                          columnNames = {"exam_schedule_id", "student_id", "subject_code"})
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExamResult extends BaseEntity {

    @Column(name = "school_id",         nullable = false) private UUID schoolId;
    @Column(name = "academic_year_id",  nullable = false) private UUID academicYearId;
    @Column(name = "exam_schedule_id",  nullable = false) private UUID examScheduleId;
    @Column(name = "student_id",        nullable = false) private UUID studentId;
    @Column(name = "section_id",        nullable = false) private UUID sectionId;
    @Column(name = "subject_code",      nullable = false, length = 30) private String subjectCode;
    @Column(name = "entered_by",        nullable = false) private UUID enteredBy;

    @Column(name = "theory_marks_obtained",   precision = 6, scale = 2) private BigDecimal theoryMarksObtained;
    @Column(name = "practical_marks_obtained",precision = 6, scale = 2) private BigDecimal practicalMarksObtained;
    @Column(name = "max_marks",  nullable = false, precision = 6, scale = 2) private BigDecimal maxMarks;
    @Column(name = "percentage", precision = 5, scale = 2)   private BigDecimal percentage;
    @Column(name = "grade", length = 5)                       private String grade;
    @Column(name = "grade_point", precision = 4, scale = 2)  private BigDecimal gradePoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 15)
    @Builder.Default
    private ResultStatus resultStatus = ResultStatus.PENDING;

    @Column(name = "remarks", length = 500) private String remarks;

    @Column(name = "is_absent")
    @Builder.Default private boolean absent = false;

    @Column(name = "is_published")
    @Builder.Default private boolean published = false;

    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "published_by") private UUID publishedBy;

    public BigDecimal getTotalMarksObtained() {
        BigDecimal theory    = theoryMarksObtained    != null ? theoryMarksObtained    : BigDecimal.ZERO;
        BigDecimal practical = practicalMarksObtained != null ? practicalMarksObtained : BigDecimal.ZERO;
        return theory.add(practical);
    }

    public enum ResultStatus { PENDING, PASS, FAIL, ABSENT, EXEMPTED, WITHHELD }
}
