package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.validation.SubmissionValidationFailure;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SubmissionStructureRepairService {

    private static final String FIX_STRUCTURE_NOT_SUPPORTED_MESSAGE =
            "This rejected submission cannot be fixed from the web editor because it needs source-content changes, not just path changes.";

    private final SubmissionRepository submissionRepository;
    private final SubmissionStorageService submissionStorageService;
    private final SubmissionValidationService validationService;
    private final SubmissionRepairAdvisor repairAdvisor;

    public SubmissionStructureRepairService(
            SubmissionRepository submissionRepository,
            SubmissionStorageService submissionStorageService,
            SubmissionValidationService validationService,
            SubmissionRepairAdvisor repairAdvisor) {
        this.submissionRepository = submissionRepository;
        this.submissionStorageService = submissionStorageService;
        this.validationService = validationService;
        this.repairAdvisor = repairAdvisor;
    }

    public boolean canFixStructure(Submission submission) {
        if (submission == null
                || submission.getStatus() != SubmissionStatus.REJECTED
                || !StringUtils.hasText(submission.getStoragePath())
                || submission.getAssignment() == null) {
            return false;
        }

        try {
            List<SubmissionFile> files = submissionStorageService.loadSubmissionFiles(submission.getStoragePath());
            return repairAdvisor.canBeRepairedFromPathsAlone(
                    submission.getAssignment(),
                    submission.getSubmitterName(),
                    files);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public Map<String, String> buildSuggestedPaths(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));
        if (!StringUtils.hasText(submission.getStoragePath())) {
            return Map.of();
        }

        List<SubmissionFile> originalFiles = submissionStorageService.loadSubmissionFiles(submission.getStoragePath());
        Optional<List<SubmissionFile>> repairCandidate = repairAdvisor.buildRepairCandidate(
                submission.getAssignment(),
                originalFiles);

        if (repairCandidate.isEmpty()) {
            return Map.of();
        }

        return repairAdvisor.buildSuggestedPaths(originalFiles, repairCandidate.get());
    }

    public void fixSubmissionStructure(Long submissionId, Map<String, String> pathMappings, List<String> deletedPaths) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (submission.getStatus() != SubmissionStatus.REJECTED) {
            throw new WorkflowStateException("Only rejected submissions can have their structure fixed.");
        }
        if (!canFixStructure(submission)) {
            throw new WorkflowStateException(FIX_STRUCTURE_NOT_SUPPORTED_MESSAGE);
        }

        Assignment assignment = submission.getAssignment();
        List<SubmissionFile> originalFiles = StringUtils.hasText(submission.getStoragePath())
                ? submissionStorageService.loadSubmissionFiles(submission.getStoragePath())
                : List.of();

        Set<String> deletedSet = new LinkedHashSet<>(deletedPaths);
        List<SubmissionFile> restructuredFiles = new ArrayList<>();
        for (SubmissionFile file : originalFiles) {
            if (deletedSet.contains(file.relativePath())) {
                continue;
            }

            String newPath = pathMappings.getOrDefault(file.relativePath(), file.relativePath());
            if (newPath == null || newPath.isBlank()) {
                continue;
            }

            String normalizedPath = newPath.replace('\\', '/').trim();
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            restructuredFiles.add(new SubmissionFile(normalizedPath, file.content()));
        }

        if (restructuredFiles.isEmpty()) {
            throw new InputValidationException("At least one file is required in the restructured submission.");
        }

        Optional<SubmissionValidationFailure> failure = validationService.validate(
                assignment,
                submission.getSubmitterName(),
                restructuredFiles);
        if (failure.isPresent()) {
            submission.setValidationCode(failure.get().code());
            submission.setValidationMessage("Structure fix failed: " + failure.get().message());
            submission.setUpdatedAt(LocalDateTime.now());
            submissionRepository.save(submission);
            throw new InputValidationException("Structure fix failed validation: " + failure.get().message());
        }

        try {
            String storagePath = submissionStorageService.storeSubmissionFiles(
                    assignment,
                    submission.getId(),
                    submission.getSubmitterName(),
                    restructuredFiles);
            submission.setStoragePath(storagePath);
            submission.setStatus(SubmissionStatus.VALIDATED);
            submission.setValidationCode(null);
            submission.setValidationMessage("Structure fixed manually. All validation checks passed.");
            submission.setFileCount(restructuredFiles.size());
            submission.setUpdatedAt(LocalDateTime.now());
            submissionRepository.save(submission);
        } catch (IllegalArgumentException ex) {
            submission.setValidationMessage("Structure fix storage failed: " + ex.getMessage());
            submission.setUpdatedAt(LocalDateTime.now());
            submissionRepository.save(submission);
            throw new InputValidationException("Structure fix failed during storage: " + ex.getMessage(), ex);
        }
    }
}
