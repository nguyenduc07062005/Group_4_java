package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.AssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private static final Set<String> DESCRIPTION_EXTENSIONS = Set.of("pdf", "md");
    private static final Set<String> OOP_RULE_EXTENSIONS = Set.of("json");

    private final AssignmentRepository assignmentRepository;
    private final SemesterRepository semesterRepository;

    public AssignmentServiceImpl(
            AssignmentRepository assignmentRepository,
            SemesterRepository semesterRepository) {
        this.assignmentRepository = assignmentRepository;
        this.semesterRepository = semesterRepository;
    }

    @Override
    @Transactional
    public Long create(AssignmentForm form) {
        Semester semester = resolveTargetSemester();

        Assignment assignment = new Assignment();
        assignment.setSemester(semester);
        assignment.setAssignmentName(form.getAssignmentName().trim());
        assignment.setGradingMode(form.getGradingMode());
        assignment.setPlagiarismThreshold(BigDecimal.valueOf(form.getPlagiarismThreshold()));
        assignment.setOutputNormalizationPolicy(form.getOutputNormalizationPolicy());

        applyDescriptionUpload(form.getDescriptionFile(), assignment);
        applyModeSpecificConfiguration(form, assignment);

        return assignmentRepository.save(assignment).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assignment> findById(Long id) {
        return assignmentRepository.findById(id);
    }

    private Semester resolveTargetSemester() {
        return semesterRepository.findFirstByArchivedFalseOrderByStartDateDesc()
                .orElseGet(this::createDefaultSemester);
    }

    private Semester createDefaultSemester() {
        LocalDate today = LocalDate.now();
        Semester semester = new Semester();
        semester.setCode(buildDefaultSemesterCode(today.getYear()));
        semester.setName("Default Semester");
        semester.setStartDate(today.withDayOfYear(1));
        semester.setEndDate(today.withMonth(12).withDayOfMonth(31));
        semester.setArchived(false);
        return semesterRepository.save(semester);
    }

    private String buildDefaultSemesterCode(int year) {
        String baseCode = "AUTO-" + year;
        String candidate = baseCode;
        int suffix = 1;

        while (semesterRepository.existsByCode(candidate)) {
            candidate = baseCode + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private void applyDescriptionUpload(MultipartFile file, Assignment assignment) {
        if (!hasUploadedFile(file)) {
            return;
        }

        String fileName = normalizeFileName(file, "descriptionFile");
        validateFileExtension(
                fileName,
                DESCRIPTION_EXTENSIONS,
                "descriptionFile",
                "Assignment description must be a .pdf or .md file.");

        byte[] fileBytes = readFileBytes(file, "descriptionFile");
        assignment.setDescriptionFileName(fileName);
        assignment.setDescriptionFileContentType(emptyToNull(file.getContentType()));
        assignment.setDescriptionFileData(fileBytes);

        if (fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            assignment.setDescription(new String(fileBytes, StandardCharsets.UTF_8));
            return;
        }

        assignment.setDescription(null);
    }

    private void applyModeSpecificConfiguration(AssignmentForm form, Assignment assignment) {
        if ("OOP".equals(form.getGradingMode())) {
            assignment.setLogicWeight(form.getLogicWeight());
            assignment.setOopWeight(form.getOopWeight());
            applyOopRuleUpload(form.getOopRuleConfig(), assignment);
            return;
        }

        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);
        assignment.setOopRuleConfigFileName(null);
        assignment.setOopRuleConfigContentType(null);
        assignment.setOopRuleConfigData(null);
    }

    private void applyOopRuleUpload(MultipartFile file, Assignment assignment) {
        if (!hasUploadedFile(file)) {
            return;
        }

        String fileName = normalizeFileName(file, "oopRuleConfig");
        validateFileExtension(
                fileName,
                OOP_RULE_EXTENSIONS,
                "oopRuleConfig",
                "OOP rule configuration must be a .json file.");

        assignment.setOopRuleConfigFileName(fileName);
        assignment.setOopRuleConfigContentType(emptyToNull(file.getContentType()));
        assignment.setOopRuleConfigData(readFileBytes(file, "oopRuleConfig"));
    }

    private boolean hasUploadedFile(MultipartFile file) {
        return file != null
                && file.getOriginalFilename() != null
                && !file.getOriginalFilename().isBlank();
    }

    private String normalizeFileName(MultipartFile file, String fieldName) {
        if (file.isEmpty()) {
            throw new AssignmentConfigValidationException(fieldName, "Uploaded file must not be empty.");
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(fileName) || fileName.contains("..")) {
            throw new AssignmentConfigValidationException(fieldName, "Uploaded file name is invalid.");
        }

        return fileName;
    }

    private void validateFileExtension(
            String fileName,
            Set<String> allowedExtensions,
            String fieldName,
            String message) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new AssignmentConfigValidationException(fieldName, message);
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new AssignmentConfigValidationException(fieldName, message);
        }
    }

    private byte[] readFileBytes(MultipartFile file, String fieldName) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new AssignmentConfigValidationException(fieldName, "Uploaded file could not be read.");
        }
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
