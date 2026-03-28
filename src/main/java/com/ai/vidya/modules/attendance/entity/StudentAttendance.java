package com.ai.vidya.modules.attendance.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "student_attendance",
    indexes = {
        @Index(name = "idx_sa_section_date", columnList = "section_id, attendance_date"),
        @Index(name = "idx_sa_student_year", columnList = "student_id, academic_year_id"),
        @Index(name = "idx_sa_date",         columnList = "attendance_date"),
        @Index(name = "idx_sa_status",       columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_att_date",
                          columnNames = {"student_id", "attendance_date", "period_number"})
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentAttendance extends BaseEntity {

    @Column(name = "school_id",        nullable = false) private UUID schoolId;
    @Column(name = "academic_year_id", nullable = false) private UUID academicYearId;
    @Column(name = "section_id",       nullable = false) private UUID sectionId;
    @Column(name = "student_id",       nullable = false) private UUID studentId;
    @Column(name = "teacher_id",       nullable = false) private UUID teacherId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** 0 = daily; 1–8 = specific period */
    @Column(name = "period_number", nullable = false)
    @Builder.Default
    private int periodNumber = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private AttendanceStatus status;

    @Column(name = "remarks", length = 200)
    private String remarks;

    @Column(name = "marked_at")
    private LocalTime markedAt;

    @Column(name = "is_corrected")
    @Builder.Default
    private boolean corrected = false;

    @Column(name = "correction_reason", length = 500)
    private String correctionReason;
}
