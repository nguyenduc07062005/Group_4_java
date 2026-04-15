package com.group4.javagrader.service.impl;

import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.GradingResultStatus;
import com.group4.javagrader.entity.OopRuleResult;
import com.group4.javagrader.entity.ProblemResult;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.TestCaseResult;
import com.group4.javagrader.grading.engine.GradingJob;
import com.group4.javagrader.grading.engine.ProblemOutcome;
import com.group4.javagrader.grading.engine.RuleCheckOutcome;
import com.group4.javagrader.grading.engine.SubmissionGraderFactory;
import com.group4.javagrader.grading.engine.SubmissionGradingOutcome;
import com.group4.javagrader.grading.engine.TestCaseOutcome;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.OopRuleResultRepository;
import com.group4.javagrader.repository.ProblemResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.GradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class GradingServiceImpl implements GradingService {

    private final BatchRepository batchRepository;
    private final SubmissionRepository submissionRepository;
    private final GradingResultRepository gradingResultRepository;
    private final ProblemResultRepository problemResultRepository;
    private final TestCaseResultRepository testCaseResultRepository;
    private final OopRuleResultRepository oopRuleResultRepository;
    private final SubmissionGraderFactory submissionGraderFactory;
    private final TransactionTemplate transactionTemplate;

    public GradingServiceImpl(
            BatchRepository batchRepository,
            SubmissionRepository submissionRepository,
            GradingResultRepository gradingResultRepository,
            ProblemResultRepository problemResultRepository,
            TestCaseResultRepository testCaseResultRepository,
            OopRuleResultRepository oopRuleResultRepository,
            SubmissionGraderFactory submissionGraderFactory,
            PlatformTransactionManager transactionManager) {
        this.batchRepository = batchRepository;
        this.submissionRepository = submissionRepository;
        this.gradingResultRepository = gradingResultRepository;
        this.problemResultRepository = problemResultRepository;
        this.testCaseResultRepository = testCaseResultRepository;
        this.oopRuleResultRepository = oopRuleResultRepository;
        this.submissionGraderFactory = submissionGraderFactory;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void processBatch(Long batchId, List<Long> gradeableSubmissionIds) {
        Batch batch = transactionTemplate.execute(status -> markReady(batchId));
        if (batch == null) {
            throw new ResourceNotFoundException("Batch not found.");
        }

        List<Submission> submissions = submissionRepository.findAllById(gradeableSubmissionIds).stream()
                .sorted(Comparator.comparing(Submission::getSubmitterName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        transactionTemplate.executeWithoutResult(status -> initializePendingResults(batch, submissions));
        transactionTemplate.executeWithoutResult(status -> markRunning(batchId));

        boolean hasSystemErrors = false;
        for (Submission submission : submissions) {
            transactionTemplate.executeWithoutResult(status -> markResultRunning(batchId, submission.getId()));
            SubmissionGradingOutcome outcome;
            try {
                outcome = submissionGraderFactory.resolve(batch.getAssignment().getGradingMode())
                        .grade(new GradingJob(batch.getAssignment(), batch, submission));
            } catch (Exception ex) {
                outcome = new SubmissionGradingOutcome(
                        "FAILED_SYSTEM",
                        zero(),
                        zero(),
                        zero(),
                        zero(),
                        0,
                        0,
                        "Unexpected grading failure.",
                        ex.getMessage(),
                        ex.getClass().getSimpleName(),
                        null,
                        List.of(),
                        List.of());
            }

            if (GradingResultStatus.FAILED_SYSTEM.name().equals(outcome.status())) {
                hasSystemErrors = true;
            }
            SubmissionGradingOutcome finalOutcome = outcome;
            transactionTemplate.executeWithoutResult(status -> persistOutcome(batchId, submission.getId(), finalOutcome));
        }

        boolean finalHasSystemErrors = hasSystemErrors;
        transactionTemplate.executeWithoutResult(status -> markCompleted(batchId, finalHasSystemErrors));
    }

    private Batch markReady(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
        batch.setStatus(BatchStatus.READY_FOR_GRADING);
        batch.setReadyAt(LocalDateTime.now());
        batch.setCompletedAt(null);
        batch.setErrorSummary(null);
        return batchRepository.save(batch);
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

    private void markRunning(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
        batch.setStatus(BatchStatus.RUNNING);
        batchRepository.save(batch);
    }

    private void markResultRunning(Long batchId, Long submissionId) {
        GradingResult gradingResult = gradingResultRepository.findByBatchIdAndSubmissionId(batchId, submissionId)
                .orElseThrow(() -> new WorkflowStateException("Result row not initialized."));
        gradingResult.setStatus(GradingResultStatus.RUNNING);
        gradingResult.setStartedAt(LocalDateTime.now());
        gradingResult.setCompletedAt(null);
        gradingResultRepository.save(gradingResult);
    }

    private void persistOutcome(Long batchId, Long submissionId, SubmissionGradingOutcome outcome) {
        GradingResult gradingResult = gradingResultRepository.findByBatchIdAndSubmissionId(batchId, submissionId)
                .orElseThrow(() -> new WorkflowStateException("Result row not found."));

        problemResultRepository.findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(gradingResult.getId())
                .forEach(problemResult -> testCaseResultRepository.findByProblemResultIdOrderByTestCaseCaseOrderAscIdAsc(problemResult.getId())
                        .forEach(testCaseResultRepository::delete));
        problemResultRepository.findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(gradingResult.getId())
                .forEach(problemResultRepository::delete);
        oopRuleResultRepository.findByGradingResultIdOrderByIdAsc(gradingResult.getId())
                .forEach(oopRuleResultRepository::delete);

        gradingResult.setStatus(GradingResultStatus.valueOf(outcome.status()));
        gradingResult.setGradingMode(gradingResult.getBatch().getAssignment().getGradingMode());
        gradingResult.setTotalScore(outcome.totalScore());
        gradingResult.setMaxScore(outcome.maxScore());
        gradingResult.setLogicScore(outcome.logicScore());
        gradingResult.setOopScore(outcome.oopScore());
        gradingResult.setTestcasePassedCount(outcome.testcasePassedCount());
        gradingResult.setTestcaseTotalCount(outcome.testcaseTotalCount());
        gradingResult.setCompileLog(outcome.compileLog());
        gradingResult.setExecutionLog(outcome.executionLog());
        gradingResult.setViolationSummary(outcome.violationSummary());
        gradingResult.setArtifactPath(outcome.artifactPath());
        if (gradingResult.getStartedAt() == null) {
            gradingResult.setStartedAt(LocalDateTime.now());
        }
        gradingResult.setCompletedAt(LocalDateTime.now());
        gradingResult = gradingResultRepository.save(gradingResult);

        for (ProblemOutcome problemOutcome : outcome.problemOutcomes()) {
            ProblemResult problemResult = new ProblemResult();
            problemResult.setGradingResult(gradingResult);
            problemResult.setProblem(problemOutcome.problem());
            problemResult.setStatus(problemOutcome.status());
            problemResult.setEarnedScore(problemOutcome.earnedScore());
            problemResult.setMaxScore(problemOutcome.maxScore());
            problemResult.setTestcasePassedCount(problemOutcome.testcasePassedCount());
            problemResult.setTestcaseTotalCount(problemOutcome.testcaseTotalCount());
            problemResult.setDetailSummary(problemOutcome.detailSummary());
            problemResult = problemResultRepository.save(problemResult);

            for (TestCaseOutcome testCaseOutcome : problemOutcome.testCaseOutcomes()) {
                TestCaseResult testCaseResult = new TestCaseResult();
                testCaseResult.setProblemResult(problemResult);
                testCaseResult.setTestCase(testCaseOutcome.testCase());
                testCaseResult.setStatus(testCaseOutcome.status());
                testCaseResult.setPassed(testCaseOutcome.passed());
                testCaseResult.setEarnedScore(testCaseOutcome.earnedScore());
                testCaseResult.setConfiguredWeight(testCaseOutcome.configuredWeight());
                testCaseResult.setExpectedOutput(testCaseOutcome.expectedOutput());
                testCaseResult.setActualOutput(testCaseOutcome.actualOutput());
                testCaseResult.setMessage(testCaseOutcome.message());
                testCaseResult.setRuntimeMillis(testCaseOutcome.runtimeMillis());
                testCaseResultRepository.save(testCaseResult);
            }
        }

        for (RuleCheckOutcome ruleOutcome : outcome.ruleOutcomes()) {
            OopRuleResult ruleResult = new OopRuleResult();
            ruleResult.setGradingResult(gradingResult);
            ruleResult.setRuleLabel(ruleOutcome.ruleLabel());
            ruleResult.setPassed(ruleOutcome.passed());
            ruleResult.setMessage(ruleOutcome.message());
            oopRuleResultRepository.save(ruleResult);
        }
    }

    private void markCompleted(Long batchId, boolean hasSystemErrors) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found."));
        batch.setStatus(hasSystemErrors ? BatchStatus.COMPLETED_WITH_ERRORS : BatchStatus.COMPLETED);
        batch.setCompletedAt(LocalDateTime.now());
        batch.setErrorSummary(hasSystemErrors ? "One or more submissions failed because of an infrastructure error." : null);
        batchRepository.save(batch);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
