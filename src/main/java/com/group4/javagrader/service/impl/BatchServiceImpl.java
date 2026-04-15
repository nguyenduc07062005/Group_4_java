package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.BatchPrecheckEntry;
import com.group4.javagrader.dto.BatchPrecheckView;
import com.group4.javagrader.dto.BatchProgressView;
import com.group4.javagrader.dto.BatchSubmissionProgressView;
import com.group4.javagrader.dto.BlockedSubmissionView;
import com.group4.javagrader.dto.PlagiarismDashboardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.GradingResultStatus;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.GradingService;
import com.group4.javagrader.service.PlagiarismService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final GradingResultRepository gradingResultRepository;
    private final PlagiarismService plagiarismService;
    private final GradingService gradingService;
    private final TransactionTemplate transactionTemplate;
    private final ThreadPoolTaskExecutor batchExecutor;
    private final int workerCount;
    private final int queueCapacity;

    public BatchServiceImpl(
            BatchRepository batchRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            GradingResultRepository gradingResultRepository,
            PlagiarismService plagiarismService,
            GradingService gradingService,
            PlatformTransactionManager transactionManager,
            @Value("${app.batch.worker-count:2}") int workerCount,
            @Value("${app.batch.queue-capacity:8}") int queueCapacity) {
        this.batchRepository = batchRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.gradingResultRepository = gradingResultRepository;
        this.plagiarismService = plagiarismService;
        this.gradingService = gradingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.workerCount = workerCount;
        this.queueCapacity = queueCapacity;
        this.batchExecutor = buildExecutor(workerCount, queueCapacity);
    }

    @Override
    @Transactional(readOnly = true)
    public BatchPrecheckView buildPrecheck(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithSemesterAndCourse(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId).stream()
                .sorted(Comparator.comparing(Submission::getSubmitterName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        PlagiarismDashboardView dashboard = plagiarismService.buildDashboard(assignmentId);
        Map<Long, BlockedSubmissionView> blockedBySubmissionId = dashboard.getBlockedSubmissions().stream()
                .collect(Collectors.toMap(BlockedSubmissionView::getSubmissionId, blocked -> blocked));

        List<BatchPrecheckEntry> gradeableEntries = new ArrayList<>();
        List<BatchPrecheckEntry> excludedEntries = new ArrayList<>();
        for (Submission submission : submissions) {
            String exclusionReason = determineExclusionReason(submission, dashboard.getLatestReport(), blockedBySubmissionId);
            if (exclusionReason == null) {
                gradeableEntries.add(new BatchPrecheckEntry(
                        submission.getId(),
                        submission.getSubmitterName(),
                        submission.getStatus().name(),
                        "Validated and cleared by the latest plagiarism report."));
            } else {
                excludedEntries.add(new BatchPrecheckEntry(
                        submission.getId(),
                        submission.getSubmitterName(),
                        submission.getStatus().name(),
                        exclusionReason));
            }
        }

        return new BatchPrecheckView(
                assignment,
                dashboard.getLatestReport(),
                gradeableEntries,
                excludedEntries,
                batchRepository.findFirstByAssignmentIdOrderByIdDesc(assignmentId).orElse(null),
                queueCapacity,
                workerCount);
    }

    @Override
    @Transactional
    public Batch createBatch(Long assignmentId, String username) {
        BatchPrecheckView precheck = buildPrecheck(assignmentId);
        if (!precheck.canCreateBatch()) {
            throw new WorkflowStateException("Run plagiarism check and ensure at least one gradeable submission before creating a batch.");
        }

        List<Long> gradeableSubmissionIds = precheck.getGradeableEntries().stream()
                .map(BatchPrecheckEntry::getSubmissionId)
                .toList();
        List<Submission> gradeableSubmissions = submissionRepository.findAllById(gradeableSubmissionIds).stream()
                .sorted(Comparator.comparing(Submission::getSubmitterName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Batch batch = new Batch();
        batch.setAssignment(precheck.getAssignment());
        batch.setPlagiarismReport(precheck.getLatestReport());
        batch.setStatus(BatchStatus.PRECHECKED);
        batch.setQueueCapacity(queueCapacity);
        batch.setWorkerCount(workerCount);
        batch.setTotalSubmissions(precheck.totalEntries());
        batch.setGradeableSubmissionCount(precheck.getGradeableEntries().size());
        batch.setExcludedSubmissionCount(precheck.getExcludedEntries().size());
        batch.setCreatedBy(defaultUsername(username));
        batch.setPrecheckSummary("Gradeable=" + precheck.getGradeableEntries().size()
                + ", Excluded=" + precheck.getExcludedEntries().size()
                + ", ReportStatus=" + precheck.getLatestReport().getStatus());
        Batch savedBatch = batchRepository.save(batch);
        initializePendingResults(savedBatch, gradeableSubmissions);
        return savedBatch;
    }

    @Override
    public Batch startBatch(Long assignmentId, Long batchId, String username) {
        String effectiveUsername = defaultUsername(username);
        BatchStartRequest startRequest = transactionTemplate.execute(status -> {
            Batch managedBatch = loadBatchForAssignmentForUpdate(assignmentId, batchId);
            if (!managedBatch.canStart()) {
                return new BatchStartRequest(managedBatch, List.of(), false);
            }

            List<Long> gradeableSubmissionIds = gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(batchId).stream()
                    .map(result -> result.getSubmission().getId())
                    .toList();
            if (gradeableSubmissionIds.isEmpty()) {
                throw new WorkflowStateException("No frozen submission snapshot is available for this batch.");
            }

            managedBatch.setStatus(BatchStatus.QUEUED);
            managedBatch.setStartedBy(effectiveUsername);
            managedBatch.setStartedAt(LocalDateTime.now());
            managedBatch.setCompletedAt(null);
            managedBatch.setErrorSummary(null);
            return new BatchStartRequest(batchRepository.save(managedBatch), gradeableSubmissionIds, true);
        });
        if (startRequest == null) {
            throw new WorkflowStateException("Batch could not be started.");
        }
        if (!startRequest.queued()) {
            return startRequest.batch();
        }

        try {
            Long queuedBatchId = startRequest.batch().getId();
            batchExecutor.execute(() -> gradingService.processBatch(queuedBatchId, startRequest.gradeableSubmissionIds()));
        } catch (TaskRejectedException ex) {
            transactionTemplate.executeWithoutResult(status -> {
                Batch managedBatch = loadBatchForAssignmentForUpdate(assignmentId, batchId);
                managedBatch.setStatus(BatchStatus.PRECHECKED);
                managedBatch.setStartedBy(null);
                managedBatch.setStartedAt(null);
                batchRepository.save(managedBatch);
            });
            throw new WorkflowStateException("Batch queue is full. Try again after the current precheck jobs finish.");
        }

        return startRequest.batch();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Batch> findById(Long batchId) {
        return batchRepository.findById(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Batch> findLatestBatch(Long assignmentId) {
        return batchRepository.findFirstByAssignmentIdOrderByIdDesc(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchProgressView> buildProgress(Long assignmentId, Long batchId) {
        Optional<Batch> batchOptional = batchRepository.findById(batchId)
                .filter(batch -> batch.getAssignment().getId().equals(assignmentId));
        if (batchOptional.isEmpty()) {
            return Optional.empty();
        }

        Batch batch = batchOptional.get();
        List<GradingResult> results = gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(batchId);
        List<BatchSubmissionProgressView> submissionViews = results.stream()
                .map(this::toProgressView)
                .toList();

        int pendingCount = (int) results.stream().filter(result -> result.getStatus() == GradingResultStatus.PENDING).count();
        int runningCount = (int) results.stream().filter(result -> result.getStatus() == GradingResultStatus.RUNNING).count();
        int completedCount = (int) results.stream().filter(GradingResult::isTerminal).count();
        int successCount = (int) results.stream().filter(GradingResult::isSuccessful).count();
        int terminalFailureCount = completedCount - successCount;

        Assignment assignment = assignmentRepository.findByIdWithSemesterAndCourse(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        return Optional.of(new BatchProgressView(
                assignment,
                batch,
                pendingCount,
                runningCount,
                completedCount,
                successCount,
                terminalFailureCount,
                submissionViews));
    }

    @PreDestroy
    public void shutdownExecutor() {
        batchExecutor.shutdown();
    }

    private ThreadPoolTaskExecutor buildExecutor(int workerCount, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("phase5-batch-");
        executor.setCorePoolSize(workerCount);
        executor.setMaxPoolSize(workerCount);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }

    private String determineExclusionReason(
            Submission submission,
            PlagiarismReport latestReport,
            Map<Long, BlockedSubmissionView> blockedBySubmissionId) {
        if (submission.isRejected()) {
            return StringUtils.hasText(submission.getValidationMessage())
                    ? submission.getValidationMessage()
                    : "Submission was rejected during upload validation.";
        }
        if (!submission.isValidated()) {
            return "Submission status " + submission.getStatus() + " is not ready for batch precheck.";
        }
        if (latestReport == null || !latestReport.isCompleted()) {
            return "Run plagiarism check before creating a batch.";
        }
        LocalDateTime latestSubmissionChangeAt = latestSubmissionChangeAt(submission);
        if (latestReport.getCompletedAt() != null
                && latestSubmissionChangeAt != null
                && latestSubmissionChangeAt.isAfter(latestReport.getCompletedAt())) {
            return "Submission was uploaded after the latest plagiarism report. Re-run plagiarism check.";
        }

        BlockedSubmissionView blockedSubmission = blockedBySubmissionId.get(submission.getId());
        if (blockedSubmission != null) {
            return blockedSubmission.getReason();
        }

        return null;
    }

    private LocalDateTime latestSubmissionChangeAt(Submission submission) {
        if (submission.getUpdatedAt() != null) {
            return submission.getUpdatedAt();
        }
        return submission.getCreatedAt();
    }

    private Batch loadBatchForAssignment(Long assignmentId, Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
        if (!batch.getAssignment().getId().equals(assignmentId)) {
            throw new OwnershipViolationException("Batch does not belong to this assignment.");
        }
        return batch;
    }

    private Batch loadBatchForAssignmentForUpdate(Long assignmentId, Long batchId) {
        return batchRepository.findByIdAndAssignmentIdForUpdate(batchId, assignmentId)
                .orElseGet(() -> {
                    Batch batch = batchRepository.findById(batchId)
                            .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
                    if (!batch.getAssignment().getId().equals(assignmentId)) {
                        throw new OwnershipViolationException("Batch does not belong to this assignment.");
                    }
                    throw new ResourceNotFoundException("Batch not found.");
                });
    }

    private void initializePendingResults(Batch batch, List<Submission> submissions) {
        for (Submission submission : submissions) {
            GradingResult gradingResult = gradingResultRepository.findByBatchIdAndSubmissionId(batch.getId(), submission.getId())
                    .orElseGet(() -> {
                        GradingResult freshResult = new GradingResult();
                        freshResult.setBatch(batch);
                        freshResult.setSubmission(submission);
                        return freshResult;
                    });
            gradingResult.setStatus(GradingResultStatus.PENDING);
            gradingResult.setGradingMode(batch.getAssignment().getGradingMode());
            gradingResult.setTotalScore(zero());
            gradingResult.setMaxScore(zero());
            gradingResult.setLogicScore(zero());
            gradingResult.setOopScore(zero());
            gradingResult.setTestcasePassedCount(0);
            gradingResult.setTestcaseTotalCount(0);
            gradingResult.setCompileLog(null);
            gradingResult.setExecutionLog(null);
            gradingResult.setViolationSummary(null);
            gradingResult.setArtifactPath(null);
            gradingResult.setStartedAt(null);
            gradingResult.setCompletedAt(null);
            gradingResultRepository.save(gradingResult);
        }
    }

    private String defaultUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "system";
    }

    private BatchSubmissionProgressView toProgressView(GradingResult result) {
        return new BatchSubmissionProgressView(
                result.getId(),
                result.getSubmission().getId(),
                result.getSubmission().getSubmitterName(),
                result.getStatus().name(),
                defaultScore(result.getTotalScore()),
                defaultScore(result.getMaxScore()),
                result.getTestcasePassedCount(),
                result.getTestcaseTotalCount(),
                result.getCompletedAt());
    }

    private BigDecimal defaultScore(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private record BatchStartRequest(Batch batch, List<Long> gradeableSubmissionIds, boolean queued) {
    }
}
