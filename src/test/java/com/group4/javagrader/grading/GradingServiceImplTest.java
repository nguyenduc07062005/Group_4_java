package com.group4.javagrader.grading;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.grading.engine.SubmissionGraderFactory;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.OopRuleResultRepository;
import com.group4.javagrader.repository.ProblemResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.impl.GradingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradingServiceImplTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private GradingResultRepository gradingResultRepository;

    @Mock
    private ProblemResultRepository problemResultRepository;

    @Mock
    private TestCaseResultRepository testCaseResultRepository;

    @Mock
    private OopRuleResultRepository oopRuleResultRepository;

    @Mock
    private SubmissionGraderFactory submissionGraderFactory;

    @Test
    void processBatchRejectsMissingBatchAsResourceNotFound() {
        GradingServiceImpl gradingService = gradingService();

        when(batchRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradingService.processBatch(404L, List.of()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Batch not found.");
    }

    @Test
    void processBatchRejectsMissingInitializedResultRowAsWorkflowState() {
        GradingServiceImpl gradingService = gradingService();
        Batch batch = batch(12L);
        Submission submission = submission(55L, "s2210001");

        when(batchRepository.findById(12L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(submissionRepository.findAllById(List.of(55L))).thenReturn(List.of(submission));
        when(gradingResultRepository.findByBatchIdAndSubmissionId(12L, 55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradingService.processBatch(12L, List.of(55L)))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Result row not initialized.");
    }

    private GradingServiceImpl gradingService() {
        return new GradingServiceImpl(
                batchRepository,
                submissionRepository,
                gradingResultRepository,
                problemResultRepository,
                testCaseResultRepository,
                oopRuleResultRepository,
                submissionGraderFactory,
                new NoOpTransactionManager());
    }

    private Batch batch(Long id) {
        Assignment assignment = new Assignment();
        assignment.setId(7L);
        assignment.setGradingMode(GradingMode.JAVA_CORE);

        Batch batch = new Batch();
        batch.setId(id);
        batch.setAssignment(assignment);
        return batch;
    }

    private Submission submission(Long id, String submitterName) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setSubmitterName(submitterName);
        return submission;
    }

    private static final class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
