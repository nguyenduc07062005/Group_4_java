package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.SemesterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;

    public SemesterServiceImpl(
            SemesterRepository semesterRepository,
            CourseRepository courseRepository,
            AssignmentRepository assignmentRepository) {
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional
    public Long create(SemesterForm form) {
        String normalizedCode = normalizeCode(form.getCode());
        if (semesterRepository.existsByCode(normalizedCode)) {
            throw new InputValidationException("Semester code already exists.");
        }

        Semester semester = new Semester();
        applyForm(semester, form, normalizedCode);

        return semesterRepository.save(semester).getId();
    }

    @Override
    @Transactional
    public void update(Long id, SemesterForm form) {
        Semester semester = resolveActiveSemester(id);
        String normalizedCode = normalizeCode(form.getCode());

        if (semesterRepository.existsByCodeAndIdNot(normalizedCode, id)) {
            throw new InputValidationException("Semester code already exists.");
        }

        applyForm(semester, form, normalizedCode);
        semesterRepository.save(semester);
    }

    @Override
    @Transactional
    public void archive(Long id) {
        Semester semester = resolveActiveSemesterForUpdate(id);
        if (courseRepository.existsBySemesterIdAndArchivedFalse(id) || assignmentRepository.existsBySemesterId(id)) {
            throw new WorkflowStateException("Cannot archive a semester that still has active courses or assignments.");
        }
        semester.setArchived(true);
        semesterRepository.save(semester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Semester> findActiveSemesters() {
        return semesterRepository.findByArchivedFalseOrderByStartDateDescIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Semester> findById(Long id) {
        return semesterRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Semester> findActiveById(Long id) {
        return semesterRepository.findByIdAndArchivedFalse(id);
    }

    private Semester resolveActiveSemester(Long id) {
        return semesterRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found."));
    }

    private Semester resolveActiveSemesterForUpdate(Long id) {
        return semesterRepository.findByIdAndArchivedFalseForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found."));
    }

    private void applyForm(Semester semester, SemesterForm form, String normalizedCode) {
        semester.setCode(normalizedCode);
        semester.setName(normalizeName(form.getName()));
        semester.setStartDate(form.getStartDate());
        semester.setEndDate(form.getEndDate());
        semester.setArchived(false);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return StringUtils.trimWhitespace(name);
    }
}
