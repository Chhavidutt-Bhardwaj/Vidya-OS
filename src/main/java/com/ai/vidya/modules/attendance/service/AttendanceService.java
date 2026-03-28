package com.ai.vidya.modules.attendance.service;

import com.ai.vidya.common.enums.AttendanceStatus;
import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.modules.attendance.dto.request.BulkAttendanceRequest;
import com.ai.vidya.modules.attendance.dto.request.StaffBulkAttendanceRequest;
import com.ai.vidya.modules.attendance.dto.response.AttendanceSummary;
import com.ai.vidya.modules.attendance.dto.response.SectionAttendanceResponse;
import com.ai.vidya.modules.attendance.entity.StaffAttendance;
import com.ai.vidya.modules.attendance.entity.StudentAttendance;
import com.ai.vidya.modules.attendance.repository.StaffAttendanceRepository;
import com.ai.vidya.modules.attendance.repository.StudentAttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StaffAttendanceRepository   staffAttendanceRepository;

    // ══════════════════════════════════════════════════════════════════════
    // STUDENT ATTENDANCE
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public void markBulkStudentAttendance(UUID schoolId, BulkAttendanceRequest req) {
        LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now();

        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Cannot mark attendance for a future date.");
        }

        // Upsert: delete existing for this section/date/period, then re-insert
        if (studentAttendanceRepository.existsBySectionAndDateAndPeriod(
                req.getSectionId(), date, req.getPeriodNumber())) {
            studentAttendanceRepository.deleteBySectionAndDateAndPeriod(
                req.getSectionId(), date, req.getPeriodNumber());
            log.info("Re-marking (correction) for section {} on {}", req.getSectionId(), date);
        }

        List<StudentAttendance> records = req.getEntries().stream()
            .map(e -> StudentAttendance.builder()
                .schoolId(schoolId)
                .academicYearId(req.getAcademicYearId())
                .sectionId(req.getSectionId())
                .studentId(e.getStudentId())
                .teacherId(req.getTeacherId())
                .attendanceDate(date)
                .periodNumber(req.getPeriodNumber())
                .status(e.getStatus())
                .remarks(e.getRemarks())
                .markedAt(LocalTime.now())
                .build())
            .toList();

        studentAttendanceRepository.saveAll(records);
        log.info("Student attendance marked: {} records in section {} for {}",
                 records.size(), req.getSectionId(), date);
    }

    @Transactional(readOnly = true)
    public AttendanceSummary getStudentSummary(UUID studentId, UUID yearId,
                                               LocalDate from, LocalDate to) {
        List<StudentAttendance> records =
            studentAttendanceRepository.findByStudentAndDateRange(studentId, yearId, from, to);

        List<StudentAttendance> daily = records.stream()
            .filter(r -> r.getPeriodNumber() == 0).toList();

        long present  = daily.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT ||
                                                    r.getStatus() == AttendanceStatus.LATE).count();
        long late     = daily.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        long onLeave  = daily.stream().filter(r -> r.getStatus() == AttendanceStatus.ON_LEAVE ||
                                                    r.getStatus() == AttendanceStatus.MEDICAL_LEAVE).count();
        long total    = daily.size();
        long absent   = total - present - onLeave;

        BigDecimal pct = total == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(present * 100.0 / total).setScale(2, RoundingMode.HALF_UP);

        return AttendanceSummary.builder()
            .studentId(studentId)
            .fromDate(from)
            .toDate(to)
            .totalWorkingDays((int) total)
            .daysPresent((int) present)
            .daysAbsent((int) absent)
            .daysLate((int) late)
            .daysOnLeave((int) onLeave)
            .attendancePercentage(pct)
            .dailyRecords(daily.stream()
                .map(r -> AttendanceSummary.DailyRecord.builder()
                    .date(r.getAttendanceDate())
                    .status(r.getStatus())
                    .remarks(r.getRemarks())
                    .build())
                .toList())
            .build();
    }

    @Transactional(readOnly = true)
    public SectionAttendanceResponse getSectionAttendance(UUID sectionId, LocalDate date, int period) {
        List<StudentAttendance> records =
            studentAttendanceRepository.findBySectionAndDate(sectionId, date, period);

        long present = records.stream()
            .filter(r -> r.getStatus() == AttendanceStatus.PRESENT ||
                         r.getStatus() == AttendanceStatus.LATE).count();

        return SectionAttendanceResponse.builder()
            .sectionId(sectionId)
            .date(date)
            .periodNumber(period)
            .totalStudents(records.size())
            .presentCount((int) present)
            .absentCount((int)(records.size() - present))
            .alreadyMarked(!records.isEmpty())
            .records(records.stream()
                .map(r -> SectionAttendanceResponse.StudentEntry.builder()
                    .studentId(r.getStudentId())
                    .status(r.getStatus())
                    .remarks(r.getRemarks())
                    .build())
                .toList())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STAFF ATTENDANCE
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public void markBulkStaffAttendance(UUID schoolId, StaffBulkAttendanceRequest req) {
        LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now();

        List<StaffAttendance> records = req.getEntries().stream()
            .map(e -> {
                // Update if exists
                StaffAttendance sa = staffAttendanceRepository
                    .findByStaffIdAndAttendanceDateAndDeletedFalse(e.getStaffId(), date)
                    .orElse(StaffAttendance.builder()
                        .schoolId(schoolId)
                        .academicYearId(req.getAcademicYearId())
                        .staffId(e.getStaffId())
                        .attendanceDate(date)
                        .build());

                sa.setStatus(e.getStatus());
                sa.setCheckInTime(e.getCheckInTime());
                sa.setCheckOutTime(e.getCheckOutTime());
                sa.setRemarks(e.getRemarks());
                sa.setLeaveType(e.getLeaveType());
                sa.setMarkedBy(req.getMarkedBy());
                return sa;
            })
            .toList();

        staffAttendanceRepository.saveAll(records);
        log.info("Staff attendance marked: {} records for {}", records.size(), date);
    }
}
