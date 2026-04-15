package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.SubmissionFileView;
import com.group4.javagrader.dto.SubmissionUploadGuide;
import com.group4.javagrader.dto.SubmissionUploadSummary;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.validation.SubmissionValidationStep;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.service.SubmissionService;
import com.group4.javagrader.service.impl.submission.SubmissionArchiveParser;
import com.group4.javagrader.service.impl.submission.SubmissionRepairAdvisor;
import com.group4.javagrader.service.impl.submission.SubmissionSnapshotGuard;
import com.group4.javagrader.service.impl.submission.SubmissionStructureRepairPlanner;
import com.group4.javagrader.service.impl.submission.SubmissionStructureRepairService;
import com.group4.javagrader.service.impl.submission.SubmissionUploadWorkflow;
import com.group4.javagrader.service.impl.submission.SubmissionValidationService;
import com.group4.javagrader.service.impl.submission.TransactionalSubmissionStorage;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionStorageService submissionStorageService;
    private final SubmissionArchiveParser archiveParser;
    private final SubmissionValidationService validationService;
    private final SubmissionUploadWorkflow uploadWorkflow;
    private final SubmissionStructureRepairService structureRepairService;

    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            BatchRepository batchRepository,
            GradingResultRepository gradingResultRepository,
            SubmissionStorageService submissionStorageService,
            List<SubmissionValidationStep> validationSteps) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionStorageService = submissionStorageService;
        this.archiveParser = new SubmissionArchiveParser();

        SubmissionValidationService createdValidationService = new SubmissionValidationService(validationSteps);
        SubmissionRepairAdvisor repairAdvisor = new SubmissionRepairAdvisor(
                new SubmissionStructureRepairPlanner(),
                createdValidationService);

        this.validationService = createdValidationService;
        this.uploadWorkflow = new SubmissionUploadWorkflow(
                submissionRepository,
                assignmentRepository,
                submissionStorageService,
                archiveParser,
                new SubmissionSnapshotGuard(batchRepository, gradingResultRepository),
                createdValidationService,
                repairAdvisor,
                new TransactionalSubmissionStorage(submissionStorageService));
        this.structureRepairService = new SubmissionStructureRepairService(
                submissionRepository,
                submissionStorageService,
                createdValidationService,
                repairAdvisor);
    }

    @Override
    @Transactional
    public SubmissionUploadSummary uploadArchive(Long assignmentId, MultipartFile archiveFile) {
        return uploadWorkflow.uploadArchive(assignmentId, archiveFile);
    }

    @Override
    public SubmissionUploadGuide getUploadGuide() {
        return validationService.buildUploadGuide(archiveParser.getMaxArchiveSizeBytes());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Submission> findByAssignmentId(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assignment> findAssignment(Long assignmentId) {
        return assignmentRepository.findByIdWithSemesterAndCourse(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Submission> findById(Long submissionId) {
        return submissionRepository.findById(submissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionFileView> loadSubmissionFilesForViewing(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));
        if (submission.getStoragePath() == null || submission.getStoragePath().isBlank()) {
            return List.of();
        }
        List<SubmissionFile> files = submissionStorageService.loadSubmissionFiles(submission.getStoragePath());
        return files.stream()
                .map(file -> new SubmissionFileView(
                        file.relativePath(),
                        file.content().length,
                        buildPreviewSnippet(file)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canFixStructure(Submission submission) {
        return structureRepairService.canFixStructure(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> buildSuggestedPaths(Long submissionId) {
        return structureRepairService.buildSuggestedPaths(submissionId);
    }

    @Override
    @Transactional
    public void fixSubmissionStructure(Long submissionId, Map<String, String> pathMappings, List<String> deletedPaths) {
        structureRepairService.fixSubmissionStructure(submissionId, pathMappings, deletedPaths);
    }

    private String buildPreviewSnippet(SubmissionFile file) {
        if (!file.relativePath().endsWith(".java") && !file.relativePath().endsWith(".txt")) {
            return "[binary file]";
        }
        String content = new String(file.content(), StandardCharsets.UTF_8);
        if (content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200) + "\n...";
    }
}
