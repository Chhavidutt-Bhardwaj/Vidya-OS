package com.ai.vidya.modules.student.service;

import com.ai.vidya.common.enums.UserType;
import com.ai.vidya.common.response.PageResponse;
import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.academic.repository.SchoolSectionRepository;
import com.ai.vidya.modules.school.repository.AcademicYearRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import com.ai.vidya.modules.student.dto.request.AdmissionRequest;
import com.ai.vidya.modules.student.dto.request.EnrollRequest;
import com.ai.vidya.modules.student.dto.request.PromoteStudentsRequest;
import com.ai.vidya.modules.student.dto.response.StudentResponse;
import com.ai.vidya.modules.student.entity.Student;
import com.ai.vidya.modules.student.entity.StudentEnrollment;
import com.ai.vidya.modules.student.entity.StudentGuardian;
import com.ai.vidya.modules.student.repository.StudentEnrollmentRepository;
import com.ai.vidya.modules.student.repository.StudentRepository;
import com.ai.vidya.modules.user.entity.Role;
import com.ai.vidya.modules.user.entity.SystemUser;
import com.ai.vidya.modules.user.repository.RoleRepository;
import com.ai.vidya.modules.user.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository           studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SchoolSectionRepository     sectionRepository;
    private final AcademicYearRepository      academicYearRepository;
    private final SchoolRepository            schoolRepository;
    private final SystemUserRepository        userRepository;
    private final RoleRepository              roleRepository;
    private final PasswordEncoder             passwordEncoder;
    private final AdmissionNumberGenerator    admissionNumberGen;

    // ══════════════════════════════════════════════════════════════════════
    // ADMISSION
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public StudentResponse admit(UUID schoolId, AdmissionRequest req) {
        requireSchool(schoolId);

        String admissionNo = (req.getAdmissionNo() != null && !req.getAdmissionNo().isBlank())
            ? req.getAdmissionNo().trim().toUpperCase()
            : admissionNumberGen.generate(schoolId);

        if (studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, admissionNo)) {
            throw new BadRequestException("Admission number '" + admissionNo + "' already exists.");
        }

        Student student = Student.builder()
            .schoolId(schoolId)
            .admissionNo(admissionNo)
            .firstName(req.getFirstName().trim())
            .middleName(req.getMiddleName())
            .lastName(req.getLastName().trim())
            .gender(req.getGender())
            .dateOfBirth(req.getDateOfBirth())
            .bloodGroup(req.getBloodGroup())
            .aadharNo(req.getAadharNo())
            .religion(req.getReligion())
            .casteCategory(req.getCasteCategory())
            .nationality(req.getNationality() != null ? req.getNationality() : "Indian")
            .personalEmail(req.getPersonalEmail())
            .mobileNo(req.getMobileNo())
            .admissionDate(req.getAdmissionDate() != null ? req.getAdmissionDate() : LocalDate.now())
            .admissionClass(req.getAdmissionClass())
            .status(Student.StudentStatus.ACTIVE)
            .build();

        if (req.getGuardians() != null) {
            req.getGuardians().forEach(g -> student.getGuardians().add(
                StudentGuardian.builder()
                    .student(student)
                    .relation(g.getRelation())
                    .fullName(g.getFullName())
                    .mobile(g.getMobile())
                    .email(g.getEmail())
                    .occupation(g.getOccupation())
                    .aadharNo(g.getAadharNo())
                    .annualIncome(g.getAnnualIncome())
                    .primary(g.isPrimary())
                    .canPickup(g.isCanPickup())
                    .build()
            ));
        }

        studentRepository.save(student);

        if (req.getSectionId() != null && req.getAcademicYearId() != null) {
            enrollInSection(schoolId, student, req.getAcademicYearId(),
                            req.getSectionId(), req.getRollNo());
        }

        provisionParentAccounts(student, schoolId);

        log.info("Student admitted: admissionNo={} school={}", admissionNo, schoolId);
        return toResponse(student, true);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ENROLLMENT
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public StudentResponse enroll(UUID schoolId, UUID studentId, EnrollRequest req) {
        Student student = requireStudent(studentId, schoolId);

        enrollmentRepository.findActiveByStudentAndYear(studentId, req.getAcademicYearId())
            .ifPresent(e -> { throw new BadRequestException(
                "Student is already enrolled in '" + e.getSectionName() +
                "' for this year. Withdraw first."); });

        enrollInSection(schoolId, student, req.getAcademicYearId(),
                        req.getSectionId(), req.getRollNo());
        return toResponse(student, true);
    }

    // ══════════════════════════════════════════════════════════════════════
    // PROMOTION
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public PromotionResult promoteStudents(UUID schoolId, PromoteStudentsRequest req) {
        List<UUID> promoted = new ArrayList<>();
        List<UUID> detained = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (PromoteStudentsRequest.StudentPromotion sp : req.getStudents()) {
            try {
                Student student = requireStudent(sp.getStudentId(), schoolId);
                StudentEnrollment current = enrollmentRepository
                    .findActiveByStudentAndYear(sp.getStudentId(), req.getFromYearId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "No active enrollment for student: " + sp.getStudentId()));

                StudentEnrollment.EnrollmentStatus newStatus = sp.isPromoted()
                    ? StudentEnrollment.EnrollmentStatus.PROMOTED
                    : StudentEnrollment.EnrollmentStatus.DETAINED;

                current.setStatus(newStatus);
                current.setPromotedOn(LocalDate.now());
                current.setPromotionRemarks(sp.getRemarks());
                enrollmentRepository.save(current);

                sectionRepository.decrementStudentCount(current.getSectionId());

                enrollInSection(schoolId, student, req.getToYearId(),
                                sp.getNextSectionId(), sp.getNextRollNo());

                if (sp.isPromoted()) promoted.add(sp.getStudentId());
                else                 detained.add(sp.getStudentId());

            } catch (Exception ex) {
                errors.add("Student " + sp.getStudentId() + ": " + ex.getMessage());
                log.error("Promotion failed for {}: {}", sp.getStudentId(), ex.getMessage());
            }
        }
        return new PromotionResult(promoted.size(), detained.size(), errors);
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUERIES
    // ══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> listBySchool(UUID schoolId, int page, int size, String search) {
        requireSchool(schoolId);
        var pageable = PageRequest.of(page, size, Sort.by("firstName", "lastName"));
        var result = (search != null && !search.isBlank())
            ? studentRepository.searchBySchoolId(schoolId, search, pageable)
            : studentRepository.findActiveBySchoolId(schoolId, pageable);

        return PageResponse.<StudentResponse>builder()
            .content(result.getContent().stream().map(s -> toResponse(s, false)).toList())
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .first(result.isFirst())
            .last(result.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public StudentResponse getById(UUID schoolId, UUID studentId) {
        return toResponse(requireStudent(studentId, schoolId), true);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listBySection(UUID schoolId, UUID sectionId, UUID yearId) {
        return studentRepository.findBySectionAndYear(schoolId, sectionId, yearId)
            .stream().map(s -> toResponse(s, false)).toList();
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE
    // ══════════════════════════════════════════════════════════════════════

    private void enrollInSection(UUID schoolId, Student student,
                                  UUID yearId, UUID sectionId, String rollNo) {
        var section = sectionRepository.findByIdNotDeleted(sectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));

        if (section.isFull()) {
            throw new BadRequestException(
                "Section '" + section.getFullName() + "' is at full capacity (" +
                section.getCapacity() + " students).");
        }

        StudentEnrollment enrollment = StudentEnrollment.builder()
            .student(student)
            .academicYearId(yearId)
            .sectionId(sectionId)
            .rollNo(rollNo)
            .className(section.getSchoolClass().getName())
            .sectionName(section.getName())
            .enrolledOn(LocalDate.now())
            .status(StudentEnrollment.EnrollmentStatus.ACTIVE)
            .build();

        student.getEnrollments().add(enrollment);
        student.setCurrentSectionId(sectionId);
        student.setCurrentRollNo(rollNo);
        student.setCurrentAcademicYearId(yearId);

        studentRepository.save(student);
        sectionRepository.incrementStudentCount(sectionId);
    }

    private void provisionParentAccounts(Student student, UUID schoolId) {
        Role parentRole = roleRepository.findSystemRoleByName("PARENT").orElse(null);
        if (parentRole == null) return;

        student.getGuardians().stream()
            .filter(g -> g.getEmail() != null && !g.getEmail().isBlank())
            .filter(g -> !userRepository.existsByEmailAndDeletedFalse(g.getEmail()))
            .forEach(g -> {
                String temp = "Parent@" + (int)(Math.random() * 90000 + 10000);
                SystemUser u = SystemUser.builder()
                    .email(g.getEmail().trim().toLowerCase())
                    .passwordHash(passwordEncoder.encode(temp))
                    .fullName(g.getFullName())
                    .phone(g.getMobile())
                    .userType(UserType.PARENT)
                    .schoolId(schoolId)
                    .active(true)
                    .mustChangePassword(true)
                    .roles(Set.of(parentRole))
                    .build();
                SystemUser saved = userRepository.save(u);
                g.setUserId(saved.getId());
                log.info("Parent portal account created: {}", g.getEmail());
            });
    }

    private void requireSchool(UUID schoolId) {
        schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
    }

    private Student requireStudent(UUID studentId, UUID schoolId) {
        Student s = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (!s.getSchoolId().equals(schoolId))
            throw new BadRequestException("Student does not belong to school: " + schoolId);
        return s;
    }

    public StudentResponse toResponse(Student s, boolean includeDetails) {
        var builder = StudentResponse.builder()
            .id(s.getId())
            .schoolId(s.getSchoolId())
            .admissionNo(s.getAdmissionNo())
            .fullName(s.getFullName())
            .firstName(s.getFirstName())
            .middleName(s.getMiddleName())
            .lastName(s.getLastName())
            .gender(s.getGender())
            .dateOfBirth(s.getDateOfBirth())
            .bloodGroup(s.getBloodGroup())
            .photoUrl(s.getPhotoUrl())
            .mobileNo(s.getMobileNo())
            .personalEmail(s.getPersonalEmail())
            .religion(s.getReligion())
            .casteCategory(s.getCasteCategory())
            .nationality(s.getNationality())
            .currentRollNo(s.getCurrentRollNo())
            .currentSectionId(s.getCurrentSectionId())
            .currentAcademicYearId(s.getCurrentAcademicYearId())
            .admissionDate(s.getAdmissionDate())
            .admissionClass(s.getAdmissionClass())
            .status(s.getStatus())
            .leavingDate(s.getLeavingDate());

        if (includeDetails) {
            builder.guardians(s.getGuardians().stream()
                .filter(g -> !Boolean.TRUE.equals(g.getDeleted()))
                .map(g -> StudentResponse.GuardianSummary.builder()
                    .fullName(g.getFullName())
                    .relation(g.getRelation())
                    .mobile(g.getMobile())
                    .email(g.getEmail())
                    .primary(g.isPrimary())
                    .canPickup(g.isCanPickup())
                    .build())
                .toList());

            builder.enrollments(s.getEnrollments().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .map(e -> StudentResponse.EnrollmentSummary.builder()
                    .academicYearId(e.getAcademicYearId())
                    .sectionId(e.getSectionId())
                    .className(e.getClassName())
                    .sectionName(e.getSectionName())
                    .rollNo(e.getRollNo())
                    .status(e.getStatus().name())
                    .enrolledOn(e.getEnrolledOn())
                    .build())
                .toList());
        }
        return builder.build();
    }

    public record PromotionResult(int promoted, int detained, List<String> errors) {}
}
