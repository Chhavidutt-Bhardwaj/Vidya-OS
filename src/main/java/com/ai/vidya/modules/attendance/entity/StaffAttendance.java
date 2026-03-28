package com.ai.vidya.modules.attendance.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.StaffAttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "staff_attendance",
    indexes = {
        @Index(name = "idx_staff_att_staff_date", columnList = "staff_id, attendance_date"),
        @Index(name = "idx_staff_att_school",     columnList = "school_id, attendance_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_staff_att_date", columnNames = {"staff_id", "attendance_date"})
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StaffAttendance extends BaseEntity {

    @Column(name = "school_id",        nullable = false) private UUID schoolId;
    @Column(name = "academic_year_id", nullable = false) private UUID academicYearId;
    @Column(name = "staff_id",         nullable = false) private UUID staffId;
    @Column(name = "marked_by",        nullable = false) private UUID markedBy;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StaffAttendanceStatus status;

    @Column(name = "check_in_time")  private LocalTime checkInTime;
    @Column(name = "check_out_time") private LocalTime checkOutTime;
    @Column(name = "remarks", length = 300) private String remarks;
    @Column(name = "leave_type", length = 30) private String leaveType;
}
