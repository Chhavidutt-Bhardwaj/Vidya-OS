package com.ai.vidya.modules.academic.service;

import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.academic.dto.request.*;
import com.ai.vidya.modules.academic.dto.response.ClassResponse;
import com.ai.vidya.modules.academic.dto.response.ClassResponse.*;
import com.ai.vidya.modules.academic.dto.response.SubjectResponse;
import com.ai.vidya.modules.academic.entity.*;
import com.ai.vidya.modules.academic.repository.*;
import com.ai.vidya.modules.school.entity.AcademicYear;
import com.ai.vidya.modules.school.entity.School;
import com.ai.vidya.modules.school.entity.SchoolShift;
import com.ai.vidya.modules.school.repository.AcademicYearRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassService {

    private final SchoolClassRepository          classRepository;
    private final SchoolSectionRepository        sectionRepository;
    private final ClassSubjectRepository         classSubjectRepository;
    private final SectionSubjectTeacherRepository teacherAssignmentRepository;
    private final SubjectRepository              subjectRepository;
    private final AcademicYearRepository         academicYearRepository;
    private final SchoolRepository               schoolRepository;
    private final SystemUserRepository           userRepository;
    private final SubjectService                 subjectService;

    // ══════════════════════════════════════════════════════════════════════
    // CLASSES
    // ══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ClassResponse> listByYear(UUID schoolId, UUID yearId) {
        requireSchool(schoolId);
        requireYear(yearId, schoolId);
        return classRepository.findAllBySchoolIdAndYearId(schoolId, yearId)
            .stream()
            .map(c -> toResponse(c, false))
            .toList();
    }

    @Transactional(readOnly = true)
    public ClassResponse getById(UUID schoolId, UUID classId) {
        SchoolClass sc = requireClass(classId, schoolId);
        return toResponse(sc, true);
    }

    @Transactional
    public ClassResponse create(UUID schoolId, ClassRequest req) {
        School      school = requireSchool(schoolId);
        AcademicYear year  = requireYear(req.getAcademicYearId(), schoolId);

        if (classRepository.existsByYearIdAndName(req.getAcademicYearId(), req.getName())) {
            throw new BadRequestException(
                "Class '" + req.getName() + "' already exists in this academic year.");
        }

        // Build class
        SchoolClass schoolClass = SchoolClass.builder()
            .school(school)
            .academicYear(year)
            .name(req.getName())
            .displayName(req.getDisplayName())
            .gradeOrder(req.getGradeOrder())
            .room(req.getRoom())
            .active(true)
            .build();

        // Inline sections
        req.getSections().forEach(sr -> {
            SchoolShift shift = sr.getShiftId() != null
                ? shiftRef(sr.getShiftId()) : null;
            schoolClass.getSections().add(SchoolSection.builder()
                .schoolClass(schoolClass)
                .name(sr.getName())
                .room(sr.getRoom())
                .capacity(sr.getCapacity())
                .classTeacherId(sr.getClassTeacherId())
                .shift(shift)
                .active(true)
                .build());
        });

        // Inline subject assignments
        req.getSubjects().forEach(sa -> {
            Subject subject = requireSubjectInSchool(sa.getSubjectId(), schoolId);
            schoolClass.getClassSubjects().add(ClassSubject.builder()
                .schoolClass(schoolClass)
                .subject(subject)
                .offeringType(sa.getOfferingType())
                .theoryPeriodsPerWeek(sa.getTheoryPeriodsPerWeek())
                .practicalPeriodsPerWeek(sa.getPracticalPeriodsPerWeek())
                .maxTheoryMarks(sa.getMaxTheoryMarks())
                .maxPracticalMarks(sa.getMaxPracticalMarks())
                .build());
        });

        classRepository.save(schoolClass);
        log.info("Class '{}' created in year '{}' school [{}]",
            schoolClass.getName(), year.getLabel(), schoolId);
        return toResponse(schoolClass, true);
    }

    @Transactional
    public ClassResponse update(UUID schoolId, UUID classId, ClassRequest req) {
        SchoolClass sc = requireClass(classId, schoolId);

        if (!sc.getName().equals(req.getName())
                && classRepository.existsByYearIdAndName(
                        sc.getAcademicYear().getId(), req.getName())) {
            throw new BadRequestException(
                "Class '" + req.getName() + "' already exists in this academic year.");
        }

        sc.setName(req.getName());
        sc.setDisplayName(req.getDisplayName());
        sc.setGradeOrder(req.getGradeOrder());
        sc.setRoom(req.getRoom());
        classRepository.save(sc);
        return toResponse(sc, true);
    }

    @Transactional
    public void delete(UUID schoolId, UUID classId) {
        SchoolClass sc = requireClass(classId, schoolId);
        sc.softDelete();
        classRepository.save(sc);
        log.info("Class '{}' deleted from school [{}]", sc.getName(), schoolId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTIONS
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public SectionResponse addSection(UUID schoolId, UUID classId, SectionRequest req) {
        SchoolClass sc = requireClass(classId, schoolId);

        if (sectionRepository.existsByClassIdAndName(classId, req.getName())) {
            throw new BadRequestException(
                "Section '" + req.getName() + "' already exists in " + sc.getName() + ".");
        }

        SchoolShift shift = req.getShiftId() != null ? shiftRef(req.getShiftId()) : null;

        SchoolSection section = SchoolSection.builder()
            .schoolClass(sc)
            .name(req.getName())
            .classTeacherId(req.getClassTeacherId())
            .room(req.getRoom())
            .capacity(req.getCapacity())
            .shift(shift)
            .active(true)
            .build();

        sectionRepository.save(section);
        log.info("Section '{}' added to class '{}' school [{}]",
            section.getName(), sc.getName(), schoolId);
        return toSectionResponse(section, false);
    }

    @Transactional
    public SectionResponse updateSection(UUID schoolId, UUID classId,
                                          UUID sectionId, SectionRequest req) {
        requireClass(classId, schoolId);
        SchoolSection section = requireSection(sectionId, classId);

        if (!section.getName().equals(req.getName())
                && sectionRepository.existsByClassIdAndName(classId, req.getName())) {
            throw new BadRequestException(
                "Section '" + req.getName() + "' already exists in this class.");
        }

        SchoolShift shift = req.getShiftId() != null ? shiftRef(req.getShiftId()) : null;

        section.setName(req.getName());
        section.setClassTeacherId(req.getClassTeacherId());
        section.setRoom(req.getRoom());
        section.setCapacity(req.getCapacity());
        section.setShift(shift);
        sectionRepository.save(section);
        return toSectionResponse(section, false);
    }

    @Transactional
    public void deleteSection(UUID schoolId, UUID classId, UUID sectionId) {
        requireClass(classId, schoolId);
        SchoolSection section = requireSection(sectionId, classId);
        if (section.getStudentCount() > 0) {
            throw new BadRequestException(
                "Cannot delete section '" + section.getName() +
                "' — it has " + section.getStudentCount() + " enrolled students. " +
                "Transfer students first.");
        }
        section.softDelete();
        sectionRepository.save(section);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUBJECT ASSIGNMENTS
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public List<ClassSubjectResponse> assignSubjects(UUID schoolId, UUID classId,
                                                       AssignSubjectsRequest req) {
        SchoolClass sc = requireClass(classId, schoolId);
        List<ClassSubjectResponse> results = new ArrayList<>();

        for (AssignSubjectsRequest.SubjectAssignment sa : req.getAssignments()) {
            if (classSubjectRepository.existsByClassIdAndSubjectId(classId, sa.getSubjectId())) {
                throw new BadRequestException(
                    "Subject " + sa.getSubjectId() + " is already assigned to " + sc.getName() + ".");
            }
            Subject subject = requireSubjectInSchool(sa.getSubjectId(), schoolId);

            ClassSubject cs = ClassSubject.builder()
                .schoolClass(sc)
                .subject(subject)
                .offeringType(sa.getOfferingType())
                .theoryPeriodsPerWeek(sa.getTheoryPeriodsPerWeek())
                .practicalPeriodsPerWeek(sa.getPracticalPeriodsPerWeek())
                .maxTheoryMarks(sa.getMaxTheoryMarks())
                .maxPracticalMarks(sa.getMaxPracticalMarks())
                .build();

            classSubjectRepository.save(cs);
            results.add(toClassSubjectResponse(cs));
        }

        log.info("{} subject(s) assigned to class '{}' school [{}]",
            results.size(), sc.getName(), schoolId);
        return results;
    }

    @Transactional
    public void removeSubject(UUID schoolId, UUID classId, UUID classSubjectId) {
        requireClass(classId, schoolId);
        ClassSubject cs = classSubjectRepository.findByIdNotDeleted(classSubjectId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Class subject assignment not found: " + classSubjectId));
        cs.softDelete();
        classSubjectRepository.save(cs);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEACHER ASSIGNMENTS
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public TeacherAssignmentResponse assignTeacher(UUID schoolId, UUID classId,
                                                    UUID sectionId,
                                                    AssignTeacherRequest req) {
        requireClass(classId, schoolId);
        SchoolSection section = requireSection(sectionId, classId);

        ClassSubject cs = classSubjectRepository.findByIdNotDeleted(req.getClassSubjectId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Class subject not found: " + req.getClassSubjectId()));

        // Ensure class subject belongs to this class
        if (!cs.getSchoolClass().getId().equals(classId)) {
            throw new BadRequestException("Class subject does not belong to this class.");
        }

        // One theory teacher per section-subject
        if (req.getAssignmentType() == SectionSubjectTeacher.AssignmentType.THEORY
                && teacherAssignmentRepository.hasTheoryTeacherForSectionSubject(
                    sectionId, req.getClassSubjectId())) {
            throw new BadRequestException(
                "A theory teacher is already assigned to this subject in " +
                section.getFullName() + ". Remove the existing assignment first.");
        }

        SectionSubjectTeacher assignment = SectionSubjectTeacher.builder()
            .section(section)
            .classSubject(cs)
            .teacherId(req.getTeacherId())
            .assignmentType(req.getAssignmentType())
            .active(true)
            .build();

        teacherAssignmentRepository.save(assignment);
        log.info("Teacher [{}] assigned to {} / {} in school [{}]",
            req.getTeacherId(), section.getFullName(),
            cs.getSubject().getName(), schoolId);
        return toTeacherAssignmentResponse(assignment);
    }

    @Transactional
    public void removeTeacherAssignment(UUID schoolId, UUID classId,
                                         UUID sectionId, UUID assignmentId) {
        requireClass(classId, schoolId);
        requireSection(sectionId, classId);
        SectionSubjectTeacher assignment =
            teacherAssignmentRepository.findByIdNotDeleted(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Teacher assignment not found: " + assignmentId));
        assignment.softDelete();
        teacherAssignmentRepository.save(assignment);
    }

    // ══════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ══════════════════════════════════════════════════════════════════════

    private ClassResponse toResponse(SchoolClass sc, boolean includeDetails) {
        int totalStudents = sc.getSections().stream()
            .filter(s -> !s.getDeleted())
            .mapToInt(SchoolSection::getStudentCount).sum();

        var builder = ClassResponse.builder()
            .id(sc.getId())
            .schoolId(sc.getSchool().getId())
            .academicYearId(sc.getAcademicYear().getId())
            .academicYearLabel(sc.getAcademicYear().getLabel())
            .name(sc.getName())
            .displayName(sc.getDisplayName())
            .gradeOrder(sc.getGradeOrder())
            .room(sc.getRoom())
            .active(sc.isActive())
            .sectionCount(sc.getSectionCount())
            .totalStudents(totalStudents);

        if (includeDetails) {
            List<SectionResponse> sections =
                sectionRepository.findAllByClassId(sc.getId())
                    .stream()
                    .map(s -> toSectionResponse(s, true))
                    .toList();

            List<ClassSubjectResponse> subjects =
                classSubjectRepository.findAllByClassId(sc.getId())
                    .stream()
                    .map(this::toClassSubjectResponse)
                    .toList();

            builder.sections(sections).subjects(subjects);
        }
        return builder.build();
    }

    private SectionResponse toSectionResponse(SchoolSection s, boolean includeAssignments) {
        String teacherName = resolveUserName(s.getClassTeacherId());
        String shiftName   = s.getShift() != null ? s.getShift().getName() : null;

        var builder = SectionResponse.builder()
            .id(s.getId())
            .name(s.getName())
            .fullName(s.getFullName())
            .classTeacherId(s.getClassTeacherId())
            .classTeacherName(teacherName)
            .room(s.getRoom())
            .capacity(s.getCapacity())
            .studentCount(s.getStudentCount())
            .availableSeats(s.availableSeats())
            .full(s.isFull())
            .active(s.isActive())
            .shiftId(s.getShift() != null ? s.getShift().getId() : null)
            .shiftName(shiftName);

        if (includeAssignments) {
            List<TeacherAssignmentResponse> assignments =
                teacherAssignmentRepository.findAllBySectionId(s.getId())
                    .stream()
                    .map(this::toTeacherAssignmentResponse)
                    .toList();
            builder.teacherAssignments(assignments);
        }
        return builder.build();
    }

    private ClassSubjectResponse toClassSubjectResponse(ClassSubject cs) {
        Subject sub = cs.getSubject();
        return ClassSubjectResponse.builder()
            .id(cs.getId())
            .subjectId(sub.getId())
            .subjectCode(sub.getCode())
            .subjectName(sub.getName())
            .subjectType(sub.getSubjectType().name())
            .offeringType(cs.getOfferingType())
            .theoryPeriodsPerWeek(cs.resolvedTheoryPeriods())
            .practicalPeriodsPerWeek(cs.resolvedPracticalPeriods())
            .maxTheoryMarks(cs.resolvedMaxTheoryMarks())
            .maxPracticalMarks(cs.resolvedMaxPracticalMarks())
            .totalMaxMarks(cs.resolvedTotalMarks())
            .colorHex(sub.getColorHex())
            .build();
    }

    private TeacherAssignmentResponse toTeacherAssignmentResponse(SectionSubjectTeacher t) {
        return TeacherAssignmentResponse.builder()
            .id(t.getId())
            .classSubjectId(t.getClassSubject().getId())
            .subjectName(t.getClassSubject().getSubject().getName())
            .teacherId(t.getTeacherId())
            .teacherName(resolveUserName(t.getTeacherId()))
            .assignmentType(t.getAssignmentType())
            .active(t.isActive())
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private School requireSchool(UUID schoolId) {
        return schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
    }

    private AcademicYear requireYear(UUID yearId, UUID schoolId) {
        AcademicYear ay = academicYearRepository.findByIdNotDeleted(yearId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Academic year not found: " + yearId));
        if (!ay.getSchool().getId().equals(schoolId)) {
            throw new BadRequestException(
                "Academic year does not belong to school: " + schoolId);
        }
        return ay;
    }

    private SchoolClass requireClass(UUID classId, UUID schoolId) {
        SchoolClass sc = classRepository.findByIdWithDetails(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        if (!sc.getSchool().getId().equals(schoolId)) {
            throw new BadRequestException("Class does not belong to school: " + schoolId);
        }
        return sc;
    }

    private SchoolSection requireSection(UUID sectionId, UUID classId) {
        SchoolSection s = sectionRepository.findByIdNotDeleted(sectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        if (!s.getSchoolClass().getId().equals(classId)) {
            throw new BadRequestException("Section does not belong to this class.");
        }
        return s;
    }

    private Subject requireSubjectInSchool(UUID subjectId, UUID schoolId) {
        Subject sub = subjectRepository.findByIdNotDeleted(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        if (!sub.getSchoolId().equals(schoolId)) {
            throw new BadRequestException("Subject does not belong to school: " + schoolId);
        }
        return sub;
    }

    private SchoolShift shiftRef(UUID shiftId) {
        SchoolShift ref = new SchoolShift();
        ref.setId(shiftId);
        return ref;
    }

    /** Resolves a user UUID to their full name. Returns null gracefully if not found. */
    private String resolveUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findByIdAndDeletedFalse(userId)
            .map(u -> u.getFullName())
            .orElse(null);
    }
}
