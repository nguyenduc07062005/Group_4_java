package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.OopRuleResultView;
import com.group4.javagrader.dto.ProblemResultView;
import com.group4.javagrader.dto.ResultDetailView;
import com.group4.javagrader.dto.ResultIndexView;
import com.group4.javagrader.dto.ResultListItemView;
import com.group4.javagrader.dto.TestCaseResultView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.OopRuleResult;
import com.group4.javagrader.entity.ProblemResult;
import com.group4.javagrader.entity.TestCaseResult;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.OopRuleResultRepository;
import com.group4.javagrader.repository.ProblemResultRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.ResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResultServiceImpl implements ResultService {

    private final AssignmentRepository assignmentRepository;
    private final BatchRepository batchRepository;
    private final GradingResultRepository gradingResultRepository;
    private final ProblemResultRepository problemResultRepository;
    private final TestCaseResultRepository testCaseResultRepository;
    private final OopRuleResultRepository oopRuleResultRepository;

    public ResultServiceImpl(
            AssignmentRepository assignmentRepository,
            BatchRepository batchRepository,
            GradingResultRepository gradingResultRepository,
            ProblemResultRepository problemResultRepository,
            TestCaseResultRepository testCaseResultRepository,
            OopRuleResultRepository oopRuleResultRepository) {
        this.assignmentRepository = assignmentRepository;
        this.batchRepository = batchRepository;
        this.gradingResultRepository = gradingResultRepository;
        this.problemResultRepository = problemResultRepository;
        this.testCaseResultRepository = testCaseResultRepository;
        this.oopRuleResultRepository = oopRuleResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultIndexView> buildIndex(Long assignmentId) {
        Optional<Assignment> assignment = assignmentRepository.findByIdWithSemesterAndCourse(assignmentId);
        if (assignment.isEmpty()) {
            return Optional.empty();
        }

        Batch latestBatch = batchRepository.findFirstByAssignmentIdAndStatusNotOrderByIdDesc(assignmentId, BatchStatus.PRECHECKED)
                .orElse(null);
        List<ResultListItemView> resultItems = latestBatch == null
                ? List.of()
                : gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(latestBatch.getId()).stream()
                .map(result -> new ResultListItemView(
                        result.getId(),
                        result.getSubmission().getId(),
                        result.getSubmission().getSubmitterName(),
                        result.getStatus().name(),
                        result.getTotalScore(),
                        result.getMaxScore(),
                        result.getLogicScore(),
                        result.getOopScore(),
                        result.getTestcasePassedCount(),
                        result.getTestcaseTotalCount(),
                        result.getCompletedAt()))
                .toList();

        return Optional.of(new ResultIndexView(assignment.get(), latestBatch, resultItems));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultDetailView> buildDetail(Long assignmentId, Long resultId) {
        Optional<Assignment> assignment = assignmentRepository.findByIdWithSemesterAndCourse(assignmentId);
        if (assignment.isEmpty()) {
            return Optional.empty();
        }

        Optional<GradingResult> gradingResultOptional = gradingResultRepository.findById(resultId)
                .filter(result -> result.getSubmission().getAssignment().getId().equals(assignmentId));
        if (gradingResultOptional.isEmpty()) {
            return Optional.empty();
        }

        GradingResult gradingResult = gradingResultOptional.get();
        if (!belongsToLatestResultsBatch(assignmentId, gradingResult)) {
            return Optional.empty();
        }

        List<ProblemResultView> problemResults = problemResultRepository.findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(resultId).stream()
                .map(this::toProblemView)
                .toList();
        List<OopRuleResultView> ruleResults = oopRuleResultRepository.findByGradingResultIdOrderByIdAsc(resultId).stream()
                .map(this::toRuleView)
                .toList();

        return Optional.of(new ResultDetailView(
                assignment.get(),
                gradingResult.getBatch(),
                gradingResult.getSubmission(),
                gradingResult,
                problemResults,
                ruleResults));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<ResultDetailView>> buildDetailsForLatestBatch(Long assignmentId) {
        Optional<Assignment> assignment = assignmentRepository.findByIdWithSemesterAndCourse(assignmentId);
        if (assignment.isEmpty()) {
            return Optional.empty();
        }

        Optional<Batch> latestBatch = batchRepository.findFirstByAssignmentIdAndStatusNotOrderByIdDesc(assignmentId, BatchStatus.PRECHECKED);
        if (latestBatch.isEmpty()) {
            return Optional.of(List.of());
        }

        List<GradingResult> gradingResults = gradingResultRepository
                .findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(latestBatch.get().getId());
        if (gradingResults.isEmpty()) {
            return Optional.of(List.of());
        }

        List<Long> gradingResultIds = gradingResults.stream()
                .map(GradingResult::getId)
                .toList();
        Map<Long, List<ProblemResultView>> problemResultsByGradingResultId = buildProblemResultsByGradingResultId(gradingResultIds);
        Map<Long, List<OopRuleResultView>> ruleResultsByGradingResultId = buildRuleResultsByGradingResultId(gradingResultIds);

        return Optional.of(gradingResults.stream()
                .map(gradingResult -> new ResultDetailView(
                        assignment.get(),
                        gradingResult.getBatch(),
                        gradingResult.getSubmission(),
                        gradingResult,
                        problemResultsByGradingResultId.getOrDefault(gradingResult.getId(), List.of()),
                        ruleResultsByGradingResultId.getOrDefault(gradingResult.getId(), List.of())))
                .toList());
    }

    private boolean belongsToLatestResultsBatch(Long assignmentId, GradingResult gradingResult) {
        return batchRepository.findFirstByAssignmentIdAndStatusNotOrderByIdDesc(assignmentId, BatchStatus.PRECHECKED)
                .map(Batch::getId)
                .filter(latestBatchId -> Objects.equals(latestBatchId, gradingResult.getBatch().getId()))
                .isPresent();
    }

    private Map<Long, List<ProblemResultView>> buildProblemResultsByGradingResultId(List<Long> gradingResultIds) {
        List<ProblemResult> problemResults = problemResultRepository
                .findByGradingResultIdInOrderByGradingResultIdAscProblemProblemOrderAscIdAsc(gradingResultIds);
        if (problemResults.isEmpty()) {
            return Map.of();
        }

        List<Long> problemResultIds = problemResults.stream()
                .map(ProblemResult::getId)
                .toList();
        Map<Long, List<TestCaseResultView>> testCaseResultsByProblemResultId = testCaseResultRepository
                .findByProblemResultIdInOrderByProblemResultIdAscTestCaseCaseOrderAscIdAsc(problemResultIds)
                .stream()
                .collect(Collectors.groupingBy(
                        result -> result.getProblemResult().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toTestCaseView, Collectors.toList())));

        return problemResults.stream()
                .collect(Collectors.groupingBy(
                        problemResult -> problemResult.getGradingResult().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                problemResult -> toProblemView(
                                        problemResult,
                                        testCaseResultsByProblemResultId.getOrDefault(problemResult.getId(), List.of())),
                                Collectors.toList())));
    }

    private Map<Long, List<OopRuleResultView>> buildRuleResultsByGradingResultId(List<Long> gradingResultIds) {
        return oopRuleResultRepository.findByGradingResultIdInOrderByGradingResultIdAscIdAsc(gradingResultIds)
                .stream()
                .collect(Collectors.groupingBy(
                        rule -> rule.getGradingResult().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toRuleView, Collectors.toList())));
    }

    private ProblemResultView toProblemView(ProblemResult problemResult) {
        List<TestCaseResultView> testCaseResults = testCaseResultRepository.findByProblemResultIdOrderByTestCaseCaseOrderAscIdAsc(problemResult.getId()).stream()
                .map(this::toTestCaseView)
                .toList();
        return toProblemView(problemResult, testCaseResults);
    }

    private ProblemResultView toProblemView(ProblemResult problemResult, List<TestCaseResultView> testCaseResults) {
        return new ProblemResultView(
                problemResult.getId(),
                problemResult.getProblem().getTitle(),
                problemResult.getProblem().getProblemOrder(),
                problemResult.getStatus(),
                problemResult.getEarnedScore(),
                problemResult.getMaxScore(),
                problemResult.getTestcasePassedCount(),
                problemResult.getTestcaseTotalCount(),
                problemResult.getDetailSummary(),
                testCaseResults);
    }

    private OopRuleResultView toRuleView(OopRuleResult rule) {
        return new OopRuleResultView(rule.getRuleLabel(), rule.isPassed(), rule.getMessage());
    }

    private TestCaseResultView toTestCaseView(TestCaseResult testCaseResult) {
        return new TestCaseResultView(
                testCaseResult.getTestCase().getCaseOrder(),
                testCaseResult.getStatus(),
                testCaseResult.isPassed(),
                testCaseResult.getEarnedScore(),
                testCaseResult.getConfiguredWeight(),
                testCaseResult.getExpectedOutput(),
                testCaseResult.getActualOutput(),
                testCaseResult.getMessage(),
                testCaseResult.getRuntimeMillis());
    }
}
