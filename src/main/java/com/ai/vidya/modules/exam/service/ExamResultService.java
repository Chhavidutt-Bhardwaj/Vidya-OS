package com.ai.vidya.modules.exam.service;

import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.modules.academic.entity.GradingScheme;
import com.ai.vidya.modules.academic.entity.GradingSchemeEntry;
import com.ai.vidya.modules.academic.repository.GradingSchemeRepository;
import com.ai.vidya.modules.attendance.service.AttendanceService;
import com.ai.vidya.modules.exam.dto.request.MarksEntryRequest;
import com.ai.vidya.modules.exam.dto.response.ReportCardResponse;
import com.ai.vidya.modules.exam.entity.ExamResult;
import com.ai.vidya.modules.exam.repository.ExamResultRepository;
import com.ai.vidya.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamResultService {

    private final ExamResultRepository   resultRepository;
    private final GradingSchemeRepository gradingSchemeRepository;
    private final StudentRepository       studentRepository;
    private final AttendanceService       attendanceService;

    // ══════════════════════════════════════════════════════════════════════
    // MARKS ENTRY
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public int enterMarks(UUID schoolId, UUID yearId, MarksEntryRequest req) {
        GradingScheme scheme = gradingSchemeRepository
            .findDefaultByAcademicYearId(yearId).orElse(null);

        List<ExamResult> results = req.getMarks().stream()
            .map(m -> {
                BigDecimal total = BigDecimal.ZERO;
                if (!m.isAbsent()) {
                    BigDecimal theory    = m.getTheoryMarks()    != null ? m.getTheoryMarks()    : BigDecimal.ZERO;
                    BigDecimal practical = m.getPracticalMarks() != null ? m.getPracticalMarks() : BigDecimal.ZERO;
                    total = theory.add(practical);
                }

                BigDecimal pct = m.getMaxMarks().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : total.multiply(BigDecimal.valueOf(100)).divide(m.getMaxMarks(), 2, RoundingMode.HALF_UP);

                String grade = scheme != null ? lookupGrade(scheme, pct) : null;

                ExamResult result = ExamResult.builder()
                    .schoolId(schoolId)
                    .academicYearId(yearId)
                    .examScheduleId(req.getExamScheduleId())
                    .studentId(m.getStudentId())
                    .sectionId(req.getSectionId())
                    .subjectCode(m.getSubjectCode())
                    .enteredBy(req.getEnteredBy())
                    .theoryMarksObtained(m.getTheoryMarks())
                    .practicalMarksObtained(m.getPracticalMarks())
                    .maxMarks(m.getMaxMarks())
                    .percentage(pct)
                    .grade(grade)
                    .absent(m.isAbsent())
                    .resultStatus(m.isAbsent()
                        ? ExamResult.ResultStatus.ABSENT
                        : pct.compareTo(BigDecimal.valueOf(35)) >= 0
                            ? ExamResult.ResultStatus.PASS
                            : ExamResult.ResultStatus.FAIL)
                    .remarks(m.getRemarks())
                    .build();
                return result;
            })
            .toList();

        resultRepository.saveAll(results);
        log.info("Marks entered: {} records for exam {} section {}", results.size(), req.getExamScheduleId(), req.getSectionId());
        return results.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUBLISH RESULTS
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public int publishResults(UUID examScheduleId, UUID publishedBy) {
        int count = resultRepository.publishByExamSchedule(examScheduleId, LocalDateTime.now(), publishedBy);
        log.info("Published {} results for exam schedule {}", count, examScheduleId);
        return count;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REPORT CARD
    // ══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ReportCardResponse generateReportCard(UUID schoolId, UUID studentId,
                                                  UUID yearId, UUID termId) {
        var student = studentRepository.findById(studentId)
            .orElseThrow(() -> new BadRequestException("Student not found: " + studentId));

        List<ExamResult> results = termId != null
            ? resultRepository.findByStudentAndYearAndTerm(studentId, yearId, termId)
            : resultRepository.findByStudentAndYear(studentId, yearId);

        GradingScheme scheme = gradingSchemeRepository.findDefaultByAcademicYearId(yearId).orElse(null);

        List<ReportCardResponse.SubjectResult> subjectResults = results.stream()
            .filter(ExamResult::isPublished)
            .map(r -> {
                String grade = (scheme != null && r.getPercentage() != null)
                    ? lookupGrade(scheme, r.getPercentage()) : r.getGrade();
                return ReportCardResponse.SubjectResult.builder()
                    .subjectCode(r.getSubjectCode())
                    .theoryMarks(r.getTheoryMarksObtained())
                    .practicalMarks(r.getPracticalMarksObtained())
                    .totalMarks(r.getTotalMarksObtained())
                    .maxMarks(r.getMaxMarks())
                    .percentage(r.getPercentage())
                    .grade(grade)
                    .gradePoint(r.getGradePoint())
                    .status(r.getResultStatus())
                    .absent(r.isAbsent())
                    .build();
            })
            .toList();

        BigDecimal totalObtained = subjectResults.stream()
            .map(ReportCardResponse.SubjectResult::getTotalMarks).filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMax = subjectResults.stream()
            .map(ReportCardResponse.SubjectResult::getMaxMarks).filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overallPct = totalMax.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
            : totalObtained.multiply(BigDecimal.valueOf(100)).divide(totalMax, 2, RoundingMode.HALF_UP);

        var attendance = attendanceService.getStudentSummary(
            studentId, yearId, LocalDate.now().withDayOfYear(1), LocalDate.now());

        return ReportCardResponse.builder()
            .studentId(studentId)
            .studentName(student.getFullName())
            .admissionNo(student.getAdmissionNo())
            .academicYearId(yearId)
            .termId(termId)
            .subjectResults(subjectResults)
            .totalMarksObtained(totalObtained)
            .totalMaxMarks(totalMax)
            .overallPercentage(overallPct)
            .overallGrade(scheme != null ? lookupGrade(scheme, overallPct) : null)
            .attendancePercentage(attendance.getAttendancePercentage())
            .daysPresent(attendance.getDaysPresent())
            .totalWorkingDays(attendance.getTotalWorkingDays())
            .passed(subjectResults.stream().noneMatch(r -> r.getStatus() == ExamResult.ResultStatus.FAIL))
            .build();
    }

    private String lookupGrade(GradingScheme scheme, BigDecimal pct) {
        if (pct == null) return null;
        return scheme.getEntries().stream()
            .filter(e -> pct.compareTo(e.getMinMarks()) >= 0 && pct.compareTo(e.getMaxMarks()) <= 0)
            .findFirst().map(GradingSchemeEntry::getGrade).orElse("N/A");
    }
}
