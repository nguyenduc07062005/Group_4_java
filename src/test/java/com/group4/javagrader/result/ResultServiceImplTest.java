package com.group4.javagrader.result;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.GradingResultStatus;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.ProblemResult;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.entity.TestCaseResult;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.OopRuleResultRepository;
import com.group4.javagrader.repository.ProblemResultRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.impl.ResultServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResultServiceImplTest {

    private final AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
    private final BatchRepository batchRepository = mock(BatchRepository.class);
    private final GradingResultRepository gradingResultRepository = mock(GradingResultRepository.class);
    private final ProblemResultRepository problemResultRepository = mock(ProblemResultRepository.class);
    private final TestCaseResultRepository testCaseResultRepository = mock(TestCaseResultRepository.class);
    private final OopRuleResultRepository oopRuleResultRepository = mock(OopRuleResultRepository.class);
    private final ResultServiceImpl resultService = new ResultServiceImpl(
            assignmentRepository,
            batchRepository,
            gradingResultRepository,
            problemResultRepository,
            testCaseResultRepository,
            oopRuleResultRepository);

    @Test
    void buildDetailRejectsResultsFromOlderBatches() {
        Assignment assignment = assignment(42L);
        when(assignmentRepository.findByIdWithSemesterAndCourse(42L)).thenReturn(Optional.of(assignment));
        when(batchRepository.findFirstByAssignmentIdAndStatusNotOrderByIdDesc(42L, BatchStatus.PRECHECKED))
                .thenReturn(Optional.of(batch(902L, assignment)));
        when(gradingResultRepository.findById(501L))
                .thenReturn(Optional.of(gradingResult(501L, assignment, batch(901L, assignment))));

        assertThat(resultService.buildDetail(42L, 501L)).isEmpty();

        verify(problemResultRepository, never()).findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(501L);
        verify(oopRuleResultRepository, never()).findByGradingResultIdOrderByIdAsc(501L);
    }

    @Test
    void buildDetailReturnsResultsFromLatestBatch() {
        Assignment assignment = assignment(42L);
        Batch latestBatch = batch(902L, assignment);
        when(assignmentRepository.findByIdWithSemesterAndCourse(42L)).thenReturn(Optional.of(assignment));
        when(batchRepository.findFirstByAssignmentIdAndStatusNotOrderByIdDesc(42L, BatchStatus.PRECHECKED))
                .thenReturn(Optional.of(latestBatch));
        when(gradingResultRepository.findById(502L))
                .thenReturn(Optional.of(gradingResult(502L, assignment, latestBatch)));
        when(problemResultRepository.findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(502L)).thenReturn(List.of());
        when(oopRuleResultRepository.findByGradingResultIdOrderByIdAsc(502L)).thenReturn(List.of());

        assertThat(resultService.buildDetail(42L, 502L)).isPresent();
    }

    @Test
    void buildDetailsForLatestBatchBulkLoadsDetailRows() {
        Assignment assignment = assignment(42L);
        Batch latestBatch = batch(902L, assignment);
        GradingResult firstResult = gradingResult(501L, assignment, latestBatch);
        GradingResult secondResult = gradingResult(502L, assignment, latestBatch);
        ProblemResult firstProblem = problemResult(601L, firstResult, "Warmup", 1);
        ProblemResult secondProblem = problemResult(602L, secondResult, "Arrays", 1);

        when(assignmentRepository.findByIdWithSemesterAndCourse(42L)).thenReturn(Optional.of(assignment));
        when(batchRepository.findFirstByAssignmentIdAndStatusNotOrderByIdDesc(42L, BatchStatus.PRECHECKED))
                .thenReturn(Optional.of(latestBatch));
        when(gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(902L))
                .thenReturn(List.of(firstResult, secondResult));
        when(problemResultRepository.findByGradingResultIdInOrderByGradingResultIdAscProblemProblemOrderAscIdAsc(List.of(501L, 502L)))
                .thenReturn(List.of(firstProblem, secondProblem));
        when(testCaseResultRepository.findByProblemResultIdInOrderByProblemResultIdAscTestCaseCaseOrderAscIdAsc(List.of(601L, 602L)))
                .thenReturn(List.of(testCaseResult(701L, firstProblem, 1), testCaseResult(702L, secondProblem, 1)));
        when(oopRuleResultRepository.findByGradingResultIdInOrderByGradingResultIdAscIdAsc(List.of(501L, 502L)))
                .thenReturn(List.of());

        var details = resultService.buildDetailsForLatestBatch(42L);

        assertThat(details).isPresent();
        assertThat(details.get()).hasSize(2);
        assertThat(details.get().get(0).getProblemResults()).hasSize(1);
        assertThat(details.get().get(0).getProblemResults().get(0).getTestCases()).hasSize(1);
        verify(gradingResultRepository, never()).findById(501L);
        verify(problemResultRepository, never()).findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(501L);
        verify(testCaseResultRepository, never()).findByProblemResultIdOrderByTestCaseCaseOrderAscIdAsc(601L);
    }

    private Assignment assignment(Long id) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        return assignment;
    }

    private Batch batch(Long id, Assignment assignment) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setAssignment(assignment);
        batch.setStatus(BatchStatus.COMPLETED);
        return batch;
    }

    private GradingResult gradingResult(Long id, Assignment assignment, Batch batch) {
        Submission submission = new Submission();
        submission.setId(id + 1000L);
        submission.setAssignment(assignment);
        submission.setSubmitterName("s2210301");

        GradingResult gradingResult = new GradingResult();
        gradingResult.setId(id);
        gradingResult.setBatch(batch);
        gradingResult.setSubmission(submission);
        gradingResult.setStatus(GradingResultStatus.DONE);
        gradingResult.setGradingMode(GradingMode.JAVA_CORE);
        return gradingResult;
    }

    private ProblemResult problemResult(Long id, GradingResult gradingResult, String title, int order) {
        Problem problem = new Problem();
        problem.setId(id + 2000L);
        problem.setTitle(title);
        problem.setProblemOrder(order);

        ProblemResult problemResult = new ProblemResult();
        problemResult.setId(id);
        problemResult.setGradingResult(gradingResult);
        problemResult.setProblem(problem);
        problemResult.setStatus("DONE");
        return problemResult;
    }

    private TestCaseResult testCaseResult(Long id, ProblemResult problemResult, int order) {
        TestCase testCase = new TestCase();
        testCase.setId(id + 3000L);
        testCase.setCaseOrder(order);

        TestCaseResult testCaseResult = new TestCaseResult();
        testCaseResult.setId(id);
        testCaseResult.setProblemResult(problemResult);
        testCaseResult.setTestCase(testCase);
        testCaseResult.setStatus("PASSED");
        testCaseResult.setPassed(true);
        return testCaseResult;
    }
}
