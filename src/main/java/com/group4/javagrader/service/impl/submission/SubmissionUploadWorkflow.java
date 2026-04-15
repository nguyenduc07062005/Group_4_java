package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.dto.SubmissionUploadSummary;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.validation.SubmissionValidationFailure;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SubmissionUploadWorkflow {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionStorageService submissionStorageService;
    private final SubmissionArchiveParser archiveParser;
    private final SubmissionSnapshotGuard snapshotGuard;
    private final SubmissionValidationService validationService;
    private final SubmissionRepairAdvisor repairAdvisor;
    private final TransactionalSubmissionStorage transactionalSubmissionStorage;

    public SubmissionUploadWorkflow(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            SubmissionStorageService submissionStorageService,
            SubmissionArchiveParser archiveParser,
            SubmissionSnapshotGuard snapshotGuard,
            SubmissionValidationService validationService,
            SubmissionRepairAdvisor repairAdvisor,
            TransactionalSubmissionStorage transactionalSubmissionStorage) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionStorageService = submissionStorageService;
        this.archiveParser = archiveParser;
        this.snapshotGuard = snapshotGuard;
        this.validationService = validationService;
        this.repairAdvisor = repairAdvisor;
        this.transactionalSubmissionStorage = transactionalSubmissionStorage;
    }

    public SubmissionUploadSummary uploadArchive(Long assignmentId, MultipartFile archiveFile) {
        Map<String, List<SubmissionFile>> groupedSubmissions = parseArchive(archiveFile);

        Assignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        snapshotGuard.rejectUploadWhenActiveBatchSnapshotWouldBeMutated(assignmentId, groupedSubmissions.keySet());

        String archiveFileName = StringUtils.cleanPath(archiveFile.getOriginalFilename());
        int validatedCount = 0;
        int rejectedCount = 0;

        for (Map.Entry<String, List<SubmissionFile>> entry : groupedSubmissions.entrySet()) {
            String submitterName = entry.getKey();
            List<SubmissionFile> files = entry.getValue();

            Submission submission = submissionRepository
                    .findByAssignmentIdAndSubmitterNameIgnoreCaseOrderByIdDesc(assignmentId, submitterName)
                    .stream()
                    .findFirst()
                    .orElseGet(Submission::new);

            snapshotGuard.rejectReuploadWhenSubmissionHasGradingSnapshot(submission);

            String previousStoragePath = submission.getStoragePath();
            submission.setAssignment(assignment);
            submission.setSubmitterName(submitterName);
            submission.setArchiveFileName(archiveFileName);
            submission.setStatus(SubmissionStatus.UPLOADED);
            submission.setFileCount(files.size());
            submission.setUpdatedAt(LocalDateTime.now());
            submission = submissionRepository.save(submission);

            Optional<SubmissionValidationFailure> failure = validationService.validate(assignment, submitterName, files);
            if (failure.isPresent()) {
                if (repairAdvisor.canBeRepairedFromPathsAlone(assignment, submitterName, files)) {
                    try {
                        String rejectedStoragePath = submissionStorageService.storeSubmissionFiles(
                                assignment,
                                submission.getId(),
                                submitterName,
                                files);
                        transactionalSubmissionStorage.deleteNewSubmissionFilesAfterRollback(
                                rejectedStoragePath,
                                previousStoragePath);
                        submission.setStoragePath(rejectedStoragePath);
                    } catch (IllegalArgumentException storeEx) {
                        submission.setStoragePath(null);
                    }
                } else {
                    submission.setStoragePath(null);
                }

                submission.setStatus(SubmissionStatus.REJECTED);
                submission.setValidationCode(failure.get().code());
                submission.setValidationMessage(failure.get().message());
                submissionRepository.save(submission);
                if (previousStoragePath != null && !previousStoragePath.equals(submission.getStoragePath())) {
                    transactionalSubmissionStorage.deleteSubmissionFilesAfterCommit(previousStoragePath);
                }
                rejectedCount++;
                continue;
            }

            try {
                String storagePath = submissionStorageService.storeSubmissionFiles(
                        assignment,
                        submission.getId(),
                        submitterName,
                        files);
                transactionalSubmissionStorage.deleteNewSubmissionFilesAfterRollback(storagePath, previousStoragePath);
                submission.setStoragePath(storagePath);
                submission.setStatus(SubmissionStatus.VALIDATED);
                submission.setValidationCode(null);
                submission.setValidationMessage("All validation checks passed.");
                submissionRepository.save(submission);
                validatedCount++;
            } catch (IllegalArgumentException ex) {
                submission.setStatus(SubmissionStatus.REJECTED);
                submission.setValidationCode("STORAGE_ERROR");
                submission.setValidationMessage(ex.getMessage());
                submission.setStoragePath(null);
                submissionRepository.save(submission);
                transactionalSubmissionStorage.deleteSubmissionFilesAfterCommit(previousStoragePath);
                rejectedCount++;
            }
        }

        return new SubmissionUploadSummary(groupedSubmissions.size(), validatedCount, rejectedCount);
    }

    private Map<String, List<SubmissionFile>> parseArchive(MultipartFile archiveFile) {
        archiveParser.validateArchiveFile(archiveFile);
        return archiveParser.parseArchive(archiveFile);
    }
}
