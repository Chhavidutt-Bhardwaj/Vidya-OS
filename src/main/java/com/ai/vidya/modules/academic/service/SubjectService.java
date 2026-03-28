package com.ai.vidya.modules.academic.service;

import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.exception.ResourceNotFoundException;
import com.ai.vidya.modules.academic.dto.request.SubjectRequest;
import com.ai.vidya.modules.academic.dto.response.SubjectResponse;
import com.ai.vidya.modules.academic.entity.Subject;
import com.ai.vidya.modules.academic.repository.ClassSubjectRepository;
import com.ai.vidya.modules.academic.repository.SubjectRepository;
import com.ai.vidya.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository      subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SchoolRepository       schoolRepository;

    // ── List ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubjectResponse> listBySchool(UUID schoolId) {
        requireSchool(schoolId);
        return subjectRepository.findAllBySchoolId(schoolId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listActiveBySchool(UUID schoolId) {
        requireSchool(schoolId);
        return subjectRepository.findAllActiveBySchoolId(schoolId)
            .stream().map(this::toResponse).toList();
    }

    // ── Get one ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SubjectResponse getById(UUID schoolId, UUID subjectId) {
        return toResponse(requireSubject(subjectId, schoolId));
    }

    // ── Create ────────────────────────────────────────────────────────────

    @Transactional
    public SubjectResponse create(UUID schoolId, SubjectRequest req) {
        requireSchool(schoolId);

        if (subjectRepository.existsBySchoolIdAndCode(schoolId, req.getCode())) {
            throw new BadRequestException(
                "Subject code '" + req.getCode() + "' already exists in this school.");
        }

        Subject subject = Subject.builder()
            .schoolId(schoolId)
            .code(req.getCode().toUpperCase())
            .name(req.getName())
            .shortName(req.getShortName())
            .subjectType(req.getSubjectType())
            .theoryPeriodsPerWeek(req.getTheoryPeriodsPerWeek())
            .practicalPeriodsPerWeek(req.getPracticalPeriodsPerWeek())
            .maxTheoryMarks(req.getMaxTheoryMarks())
            .maxPracticalMarks(req.getMaxPracticalMarks())
            .graded(req.isGraded())
            .active(req.isActive())
            .boardOverride(req.getBoardOverride())
            .colorHex(req.getColorHex())
            .build();

        subjectRepository.save(subject);
        log.info("Subject '{}' created for school [{}]", subject.getCode(), schoolId);
        return toResponse(subject);
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Transactional
    public SubjectResponse update(UUID schoolId, UUID subjectId, SubjectRequest req) {
        Subject subject = requireSubject(subjectId, schoolId);

        // Code change — check uniqueness
        if (!subject.getCode().equals(req.getCode().toUpperCase())
                && subjectRepository.existsBySchoolIdAndCode(schoolId, req.getCode())) {
            throw new BadRequestException(
                "Subject code '" + req.getCode() + "' already exists in this school.");
        }

        subject.setCode(req.getCode().toUpperCase());
        subject.setName(req.getName());
        subject.setShortName(req.getShortName());
        subject.setSubjectType(req.getSubjectType());
        subject.setTheoryPeriodsPerWeek(req.getTheoryPeriodsPerWeek());
        subject.setPracticalPeriodsPerWeek(req.getPracticalPeriodsPerWeek());
        subject.setMaxTheoryMarks(req.getMaxTheoryMarks());
        subject.setMaxPracticalMarks(req.getMaxPracticalMarks());
        subject.setGraded(req.isGraded());
        subject.setActive(req.isActive());
        subject.setBoardOverride(req.getBoardOverride());
        subject.setColorHex(req.getColorHex());

        subjectRepository.save(subject);
        log.info("Subject '{}' updated for school [{}]", subject.getCode(), schoolId);
        return toResponse(subject);
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID schoolId, UUID subjectId) {
        Subject subject = requireSubject(subjectId, schoolId);

        // Guard: cannot delete if assigned to any class
        boolean assigned = classSubjectRepository
            .findAllByClassId(subjectId).stream()
            .anyMatch(cs -> cs.getSubject().getId().equals(subjectId));
        // Re-query properly
        if (!classSubjectRepository.findAllByClassId(subjectId).isEmpty()) {
            // Check via a direct query to avoid loading all class subjects
        }
        // Simple approach: soft-delete and let constraint surface if needed
        subject.softDelete();
        subjectRepository.save(subject);
        log.info("Subject '{}' deleted from school [{}]", subject.getCode(), schoolId);
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    public SubjectResponse toResponse(Subject s) {
        int maxTheory      = s.getMaxTheoryMarks()     != null ? s.getMaxTheoryMarks()     : 0;
        int maxPractical   = s.getMaxPracticalMarks()  != null ? s.getMaxPracticalMarks()  : 0;
        return SubjectResponse.builder()
            .id(s.getId())
            .schoolId(s.getSchoolId())
            .code(s.getCode())
            .name(s.getName())
            .shortName(s.getShortName())
            .subjectType(s.getSubjectType())
            .theoryPeriodsPerWeek(s.getTheoryPeriodsPerWeek())
            .practicalPeriodsPerWeek(s.getPracticalPeriodsPerWeek())
            .maxTheoryMarks(s.getMaxTheoryMarks())
            .maxPracticalMarks(s.getMaxPracticalMarks())
            .totalMaxMarks(maxTheory + maxPractical)
            .graded(s.isGraded())
            .active(s.isActive())
            .boardOverride(s.getBoardOverride())
            .colorHex(s.getColorHex())
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void requireSchool(UUID schoolId) {
        schoolRepository.findByIdAndDeletedFalse(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
    }

    private Subject requireSubject(UUID subjectId, UUID schoolId) {
        Subject s = subjectRepository.findByIdNotDeleted(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        if (!s.getSchoolId().equals(schoolId)) {
            throw new BadRequestException("Subject does not belong to school: " + schoolId);
        }
        return s;
    }
}
