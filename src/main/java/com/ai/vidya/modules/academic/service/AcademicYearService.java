package com.ai.vidya.modules.academic.service;

import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.academic.dto.request.AcademicYearRequest;
import com.ai.vidya.modules.academic.dto.request.ShiftRequest;
import com.ai.vidya.modules.academic.dto.response.AcademicYearResponse;
import com.ai.vidya.modules.academic.repository.SchoolShiftRepository;
import com.ai.vidya.modules.academic.repository.SchoolTermRepository;
import com.ai.vidya.modules.school.entity.*;
import com.ai.vidya.modules.school.repository.AcademicYearRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final SchoolTermRepository   termRepository;
    private final SchoolShiftRepository  shiftRepository;
    private final SchoolRepository       schoolRepository;

    // ══════════════════════════════════════════════════════════════════════
    // ACADEMIC YEAR CRUD
    // ══════════════════════════════════════════════════════════════════════

    /** GET /api/v1/schools/{schoolId}/academic-years */
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> listBySchool(UUID schoolId) {
        requireSchool(schoolId);
        return academicYearRepository.findAllBySchoolId(schoolId)
            .stream()
            .map(ay -> toResponse(ay, false))
            .toList();
    }

    /** GET /api/v1/schools/{schoolId}/academic-years/current */
    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrent(UUID schoolId) {
        requireSchool(schoolId);
        AcademicYear year = academicYearRepository.findCurrentBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No current academic year set for school: " + schoolId));
        return toResponse(year, true);
    }

    /** GET /api/v1/schools/{schoolId}/academic-years/{yearId} */
    @Transactional(readOnly = true)
    public AcademicYearResponse getById(UUID schoolId, UUID yearId) {
        AcademicYear year = requireYear(yearId, schoolId);
        return toResponse(year, true);
    }

    /** POST /api/v1/schools/{schoolId}/academic-years */
    @Transactional
    public AcademicYearResponse create(UUID schoolId, AcademicYearRequest req) {
        School school = requireSchool(schoolId);

        // ── Validations ──────────────────────────────────────────────────
        validateDateRange(req.getStartDate(), req.getEndDate(), "Academic year");

        if (academicYearRepository.existsBySchoolIdAndLabel(schoolId, req.getLabel())) {
            throw new BadRequestException(
                "Academic year '" + req.getLabel() + "' already exists for this school.");
        }
        validateTerms(req.getTerms(), req.getStartDate(), req.getEndDate());
        validateShifts(req.getShifts());

        // ── If making current, clear existing current flag ───────────────
        if (req.isMakeCurrent()) {
            academicYearRepository.clearCurrentFlagForSchool(schoolId);
        }

        // ── Build AcademicYear ───────────────────────────────────────────
        AcademicYear year = AcademicYear.builder()
            .school(school)
            .label(req.getLabel())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .current(req.isMakeCurrent())
            .locked(false)
            .build();

        // ── Inline terms ─────────────────────────────────────────────────
        req.getTerms().forEach(t -> year.getTerms().add(
            SchoolTerm.builder()
                .academicYear(year)
                .name(t.getName())
                .sortOrder(t.getSortOrder())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .build()
        ));

        // ── Inline shifts ────────────────────────────────────────────────
        boolean hasDefaultShift = req.getShifts().stream()
            .anyMatch(AcademicYearRequest.ShiftRequest::isDefaultShift);

        for (int i = 0; i < req.getShifts().size(); i++) {
            AcademicYearRequest.ShiftRequest s = req.getShifts().get(i);
            // Auto-mark first shift as default if none explicitly flagged
            boolean isDefault = s.isDefaultShift() || (!hasDefaultShift && i == 0);
            year.getShifts().add(
                SchoolShift.builder()
                    .academicYear(year)
                    .name(s.getName())
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .defaultShift(isDefault)
                    .build()
            );
        }

        academicYearRepository.save(year);
        log.info("Academic year '{}' created for school [{}]", year.getLabel(), schoolId);
        return toResponse(year, true);
    }

    /** PUT /api/v1/schools/{schoolId}/academic-years/{yearId} */
    @Transactional
    public AcademicYearResponse update(UUID schoolId, UUID yearId, AcademicYearRequest req) {
        AcademicYear year = requireYear(yearId, schoolId);

        if (year.isLocked()) {
            throw new BadRequestException(
                "Academic year '" + year.getLabel() + "' is locked and cannot be modified.");
        }
        validateDateRange(req.getStartDate(), req.getEndDate(), "Academic year");

        // Label change — check uniqueness if label actually changed
        if (!year.getLabel().equals(req.getLabel())
                && academicYearRepository.existsBySchoolIdAndLabel(schoolId, req.getLabel())) {
            throw new BadRequestException(
                "Academic year '" + req.getLabel() + "' already exists for this school.");
        }

        if (req.isMakeCurrent() && !year.isCurrent()) {
            academicYearRepository.clearCurrentFlagForSchool(schoolId);
            year.setCurrent(true);
        }

        year.setLabel(req.getLabel());
        year.setStartDate(req.getStartDate());
        year.setEndDate(req.getEndDate());
        academicYearRepository.save(year);

        log.info("Academic year '{}' updated for school [{}]", year.getLabel(), schoolId);
        return toResponse(year, true);
    }

    /** PATCH /api/v1/schools/{schoolId}/academic-years/{yearId}/set-current */
    @Transactional
    public AcademicYearResponse setAsCurrent(UUID schoolId, UUID yearId) {
        requireSchool(schoolId);
        AcademicYear year = requireYear(yearId, schoolId);

        if (year.isCurrent()) {
            throw new BadRequestException(
                "Academic year '" + year.getLabel() + "' is already the current year.");
        }

        academicYearRepository.clearCurrentFlagForSchool(schoolId);
        year.setCurrent(true);
        academicYearRepository.save(year);

        log.info("Academic year '{}' set as current for school [{}]", year.getLabel(), schoolId);
        return toResponse(year, true);
    }

    /** PATCH /api/v1/schools/{schoolId}/academic-years/{yearId}/lock */
    @Transactional
    public AcademicYearResponse lock(UUID schoolId, UUID yearId) {
        AcademicYear year = requireYear(yearId, schoolId);
        if (year.isLocked()) {
            throw new BadRequestException("Academic year is already locked.");
        }
        year.setLocked(true);
        academicYearRepository.save(year);
        log.info("Academic year '{}' locked for school [{}]", year.getLabel(), schoolId);
        return toResponse(year, true);
    }

    /** DELETE /api/v1/schools/{schoolId}/academic-years/{yearId} */
    @Transactional
    public void delete(UUID schoolId, UUID yearId) {
        AcademicYear year = requireYear(yearId, schoolId);
        if (year.isLocked()) {
            throw new BadRequestException(
                "Academic year '" + year.getLabel() + "' is locked and cannot be deleted.");
        }
        if (year.isCurrent()) {
            throw new BadRequestException(
                "Cannot delete the current academic year. " +
                "Set another year as current first.");
        }
        year.softDelete();
        academicYearRepository.save(year);
        log.info("Academic year '{}' soft-deleted for school [{}]", year.getLabel(), schoolId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TERMS
    // ══════════════════════════════════════════════════════════════════════

    /** POST /api/v1/schools/{schoolId}/academic-years/{yearId}/terms */
    @Transactional
    public AcademicYearResponse.TermResponse addTerm(UUID schoolId, UUID yearId,
                                                      AcademicYearRequest.TermRequest req) {
        AcademicYear year = requireYear(yearId, schoolId);
        requireNotLocked(year);
        validateDateRange(req.getStartDate(), req.getEndDate(), "Term");
        validateTermWithinYear(req.getStartDate(), req.getEndDate(), year);

        if (termRepository.existsByAcademicYearIdAndName(yearId, req.getName())) {
            throw new BadRequestException(
                "A term named '" + req.getName() + "' already exists in this academic year.");
        }

        SchoolTerm term = SchoolTerm.builder()
            .academicYear(year)
            .name(req.getName())
            .sortOrder(req.getSortOrder())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .build();

        termRepository.save(term);
        log.info("Term '{}' added to year '{}' school [{}]",
            term.getName(), year.getLabel(), schoolId);
        return toTermResponse(term);
    }

    /** PUT /api/v1/schools/{schoolId}/academic-years/{yearId}/terms/{termId} */
    @Transactional
    public AcademicYearResponse.TermResponse updateTerm(UUID schoolId, UUID yearId,
                                                         UUID termId, AcademicYearRequest.TermRequest req) {
        AcademicYear year = requireYear(yearId, schoolId);
        requireNotLocked(year);
        SchoolTerm term = requireTerm(termId, yearId);

        validateDateRange(req.getStartDate(), req.getEndDate(), "Term");
        validateTermWithinYear(req.getStartDate(), req.getEndDate(), year);

        // Name change — uniqueness check
        if (!term.getName().equals(req.getName())
                && termRepository.existsByAcademicYearIdAndName(yearId, req.getName())) {
            throw new BadRequestException(
                "A term named '" + req.getName() + "' already exists in this academic year.");
        }

        term.setName(req.getName());
        term.setSortOrder(req.getSortOrder());
        term.setStartDate(req.getStartDate());
        term.setEndDate(req.getEndDate());
        termRepository.save(term);
        return toTermResponse(term);
    }

    /** DELETE /api/v1/schools/{schoolId}/academic-years/{yearId}/terms/{termId} */
    @Transactional
    public void deleteTerm(UUID schoolId, UUID yearId, UUID termId) {
        AcademicYear year = requireYear(yearId, schoolId);
        requireNotLocked(year);
        SchoolTerm term = requireTerm(termId, yearId);
        if (term.isLocked()) {
            throw new BadRequestException("Term '" + term.getName() + "' is locked.");
        }
        term.softDelete();
        termRepository.save(term);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SHIFTS
    // ══════════════════════════════════════════════════════════════════════

    /** POST /api/v1/schools/{schoolId}/academic-years/{yearId}/shifts */
    @Transactional
    public AcademicYearResponse.ShiftResponse addShift(UUID schoolId, UUID yearId,
                                                        ShiftRequest req) {
        AcademicYear year = requireYear(yearId, schoolId);
        requireNotLocked(year);
        validateShiftTime(req.getStartTime(), req.getEndTime());

        if (shiftRepository.existsByAcademicYearIdAndName(yearId, req.getName())) {
            throw new BadRequestException(
                "A shift named '" + req.getName() + "' already exists in this academic year.");
        }

        if (req.isDefaultShift()) {
            shiftRepository.clearDefaultFlagForYear(yearId);
        }

        // If this is the first shift, make it default automatically
        boolean isFirst = shiftRepository.findAllByAcademicYearId(yearId).isEmpty();

        SchoolShift shift = SchoolShift.builder()
            .academicYear(year)
            .name(req.getName())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .defaultShift(req.isDefaultShift() || isFirst)
            .build();

        shiftRepository.save(shift);
        log.info("Shift '{}' added to year '{}' school [{}]",
            shift.getName(), year.getLabel(), schoolId);
        return toShiftResponse(shift);
    }

    /** PUT /api/v1/schools/{schoolId}/academic-years/{yearId}/shifts/{shiftId} */
    @Transactional
    public AcademicYearResponse.ShiftResponse updateShift(UUID schoolId, UUID yearId,
                                                           UUID shiftId, ShiftRequest req) {
        AcademicYear year = requireYear(yearId, schoolId);
        requireNotLocked(year);
        SchoolShift shift = requireShift(shiftId, yearId);
        validateShiftTime(req.getStartTime(), req.getEndTime());

        if (!shift.getName().equals(req.getName())
                && shiftRepository.existsByAcademicYearIdAndName(yearId, req.getName())) {
            throw new BadRequestException(
                "A shift named '" + req.getName() + "' already exists in this academic year.");
        }

        if (req.isDefaultShift() && !shift.isDefaultShift()) {
            shiftRepository.clearDefaultFlagForYear(yearId);
        }

        shift.setName(req.getName());
        shift.setStartTime(req.getStartTime());
        shift.setEndTime(req.getEndTime());
        shift.setDefaultShift(req.isDefaultShift());
        shiftRepository.save(shift);
        return toShiftResponse(shift);
    }

    /** DELETE /api/v1/schools/{schoolId}/academic-years/{yearId}/shifts/{shiftId} */
    @Transactional
    public void deleteShift(UUID schoolId, UUID yearId, UUID shiftId) {
        AcademicYear year = requireYear(yearId, schoolId);
        requireNotLocked(year);
        SchoolShift shift = requireShift(shiftId, yearId);

        if (shift.isDefaultShift()) {
            throw new BadRequestException(
                "Cannot delete the default shift. Assign another shift as default first.");
        }
        shift.softDelete();
        shiftRepository.save(shift);
    }

    // ══════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ══════════════════════════════════════════════════════════════════════

    private AcademicYearResponse toResponse(AcademicYear ay, boolean includeChildren) {
        LocalDate today = LocalDate.now();
        var builder = AcademicYearResponse.builder()
            .id(ay.getId())
            .schoolId(ay.getSchool().getId())
            .label(ay.getLabel())
            .startDate(ay.getStartDate())
            .endDate(ay.getEndDate())
            .current(ay.isCurrent())
            .locked(ay.isLocked())
            .started(!today.isBefore(ay.getStartDate()))
            .ended(today.isAfter(ay.getEndDate()))
            .totalDays(ChronoUnit.DAYS.between(ay.getStartDate(), ay.getEndDate()) + 1);

        if (includeChildren) {
            List<AcademicYearResponse.TermResponse> terms =
                termRepository.findAllByAcademicYearId(ay.getId())
                    .stream().map(this::toTermResponse).toList();

            List<AcademicYearResponse.ShiftResponse> shifts =
                shiftRepository.findAllByAcademicYearId(ay.getId())
                    .stream().map(this::toShiftResponse).toList();

            builder.terms(terms).shifts(shifts);
        }
        return builder.build();
    }

    private AcademicYearResponse.TermResponse toTermResponse(SchoolTerm t) {
        LocalDate today = LocalDate.now();
        return AcademicYearResponse.TermResponse.builder()
            .id(t.getId())
            .name(t.getName())
            .sortOrder(t.getSortOrder())
            .startDate(t.getStartDate())
            .endDate(t.getEndDate())
            .locked(t.isLocked())
            .current(!today.isBefore(t.getStartDate()) && !today.isAfter(t.getEndDate()))
            .totalDays(ChronoUnit.DAYS.between(t.getStartDate(), t.getEndDate()) + 1)
            .build();
    }

    private AcademicYearResponse.ShiftResponse toShiftResponse(SchoolShift s) {
        long minutes = ChronoUnit.MINUTES.between(s.getStartTime(), s.getEndTime());
        String duration = String.format("%dh %dm", minutes / 60, minutes % 60);
        return AcademicYearResponse.ShiftResponse.builder()
            .id(s.getId())
            .name(s.getName())
            .startTime(s.getStartTime())
            .endTime(s.getEndTime())
            .defaultShift(s.isDefaultShift())
            .duration(duration)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void validateDateRange(LocalDate start, LocalDate end, String label) {
        if (!end.isAfter(start)) {
            throw new BadRequestException(label + " end date must be after start date.");
        }
    }

    private void validateTermWithinYear(LocalDate termStart, LocalDate termEnd,
                                         AcademicYear year) {
        if (termStart.isBefore(year.getStartDate()) || termEnd.isAfter(year.getEndDate())) {
            throw new BadRequestException(
                "Term dates must fall within the academic year (" +
                year.getStartDate() + " – " + year.getEndDate() + ").");
        }
    }

    private void validateShiftTime(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("Shift end time must be after start time.");
        }
    }

    private void validateTerms(List<AcademicYearRequest.TermRequest> terms,
                                LocalDate yearStart, LocalDate yearEnd) {
        for (var t : terms) {
            validateDateRange(t.getStartDate(), t.getEndDate(), "Term '" + t.getName() + "'");
            if (t.getStartDate().isBefore(yearStart) || t.getEndDate().isAfter(yearEnd)) {
                throw new BadRequestException(
                    "Term '" + t.getName() + "' dates must fall within the academic year.");
            }
        }
    }

    private void validateShifts(List<AcademicYearRequest.ShiftRequest> shifts) {
        long defaultCount = shifts.stream()
            .filter(AcademicYearRequest.ShiftRequest::isDefaultShift).count();
        if (defaultCount > 1) {
            throw new BadRequestException("Only one shift can be marked as default.");
        }
        for (var s : shifts) {
            validateShiftTime(s.getStartTime(), s.getEndTime());
        }
    }

    private void requireNotLocked(AcademicYear year) {
        if (year.isLocked()) {
            throw new BadRequestException(
                "Academic year '" + year.getLabel() + "' is locked and cannot be modified.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FINDERS
    // ══════════════════════════════════════════════════════════════════════

    private School requireSchool(UUID schoolId) {
        return schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
    }

    private AcademicYear requireYear(UUID yearId, UUID schoolId) {
        AcademicYear year = academicYearRepository.findByIdWithDetails(yearId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found: " + yearId));
        if (!year.getSchool().getId().equals(schoolId)) {
            throw new BadRequestException(
                "Academic year does not belong to school: " + schoolId);
        }
        return year;
    }

    private SchoolTerm requireTerm(UUID termId, UUID yearId) {
        SchoolTerm term = termRepository.findByIdNotDeleted(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termId));
        if (!term.getAcademicYear().getId().equals(yearId)) {
            throw new BadRequestException("Term does not belong to this academic year.");
        }
        return term;
    }

    private SchoolShift requireShift(UUID shiftId, UUID yearId) {
        SchoolShift shift = shiftRepository.findByIdNotDeleted(shiftId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + shiftId));
        if (!shift.getAcademicYear().getId().equals(yearId)) {
            throw new BadRequestException("Shift does not belong to this academic year.");
        }
        return shift;
    }
}
