package com.group4.javagrader.plagiarism;

import com.group4.javagrader.dto.BatchPrecheckView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.service.GradingService;
import com.group4.javagrader.service.PlagiarismService;
import com.group4.javagrader.service.impl.BatchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceImplTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private GradingResultRepository gradingResultRepository;

    @Mock
    private PlagiarismService plagiarismService;

    @Mock
    private GradingService gradingService;

    @Test
    void startBatchQueuesOnlyOnceWhenTwoRequestsRace() throws Exception {
        long assignmentId = 12L;
        long batchId = 34L;
        long submissionId = 56L;

        LockHarness lockHarness = new LockHarness();
        PlatformTransactionManager transactionManager = new LockAwareTransactionManager(lockHarness);
        BatchServiceImpl batchService = new BatchServiceImpl(
                batchRepository,
                assignmentRepository,
                submissionRepository,
                gradingResultRepository,
                plagiarismService,
                gradingService,
                transactionManager,
                2,
                8);

        AtomicReference<BatchStatus> persistedStatus = new AtomicReference<>(BatchStatus.PRECHECKED);

        when(batchRepository.findByIdAndAssignmentIdForUpdate(batchId, assignmentId)).thenAnswer(invocation -> {
            lockHarness.lock();
            return Optional.of(batch(batchId, assignmentId, persistedStatus.get()));
        });

        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch savedBatch = copyBatch(invocation.getArgument(0));
            persistedStatus.set(savedBatch.getStatus());
            return savedBatch;
        });

        GradingResult gradingResult = new GradingResult();
        Submission submission = new Submission();
        submission.setId(submissionId);
        gradingResult.setSubmission(submission);
        when(gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(batchId))
                .thenReturn(List.of(gradingResult));

        ExecutorService callers = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        Callable<Batch> startCall = () -> {
            startGate.await(5, TimeUnit.SECONDS);
            return batchService.startBatch(assignmentId, batchId, "teacher");
        };

        Future<Batch> first = callers.submit(startCall);
        Future<Batch> second = callers.submit(startCall);
        startGate.countDown();

        Batch firstResult = first.get(5, TimeUnit.SECONDS);
        Batch secondResult = second.get(5, TimeUnit.SECONDS);

        assertThat(firstResult.getStatus()).isEqualTo(BatchStatus.QUEUED);
        assertThat(secondResult.getStatus()).isIn(BatchStatus.QUEUED, BatchStatus.PRECHECKED);
        assertThat(persistedStatus.get()).isEqualTo(BatchStatus.QUEUED);
        verify(gradingService, after(1000).times(1)).processBatch(eq(batchId), eq(List.of(submissionId)));

        callers.shutdownNow();
        batchService.shutdownExecutor();
    }

    @Test
    void buildPrecheckRejectsMissingAssignmentWithTypedNotFoundException() {
        BatchServiceImpl batchService = new BatchServiceImpl(
                batchRepository,
                assignmentRepository,
                submissionRepository,
                gradingResultRepository,
                plagiarismService,
                gradingService,
                new NoOpTransactionManager(),
                2,
                8);

        when(assignmentRepository.findById(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.buildPrecheck(12L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void createBatchRejectsUnreadyPrecheckWithTypedWorkflowException() {
        BatchServiceImpl batchService = new BatchServiceImpl(
                batchRepository,
                assignmentRepository,
                submissionRepository,
                gradingResultRepository,
                plagiarismService,
                gradingService,
                new NoOpTransactionManager(),
                2,
                8);

        Assignment assignment = new Assignment();
        assignment.setId(12L);
        PlagiarismReport report = completedReport(12L);
        BatchPrecheckView precheck = new BatchPrecheckView(
                assignment,
                report,
                List.of(),
                List.of(),
                null,
                8,
                2);

        when(assignmentRepository.findById(12L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(12L)).thenReturn(List.of());
        when(plagiarismService.buildDashboard(12L)).thenReturn(new com.group4.javagrader.dto.PlagiarismDashboardView(
                assignment,
                List.of(),
                report,
                List.of(),
                List.of()));
        when(batchRepository.findFirstByAssignmentIdOrderByIdDesc(12L)).thenReturn(Optional.empty());

        assertThat(precheck.canCreateBatch()).isFalse();
        assertThatThrownBy(() -> batchService.createBatch(12L, "teacher"))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Run plagiarism check and ensure at least one gradeable submission before creating a batch.");
    }

    private static PlagiarismReport completedReport(long assignmentId) {
        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);

        PlagiarismReport report = new PlagiarismReport();
        report.setAssignment(assignment);
        report.setStatus("COMPLETED");
        return report;
    }

    private static Batch batch(long batchId, long assignmentId, BatchStatus status) {
        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setAssignment(assignment);
        batch.setStatus(status);
        return batch;
    }

    private static Batch copyBatch(Batch source) {
        Batch copy = batch(source.getId(), source.getAssignment().getId(), source.getStatus());
        copy.setStartedBy(source.getStartedBy());
        copy.setStartedAt(source.getStartedAt() != null ? LocalDateTime.from(source.getStartedAt()) : null);
        copy.setCompletedAt(source.getCompletedAt() != null ? LocalDateTime.from(source.getCompletedAt()) : null);
        copy.setErrorSummary(source.getErrorSummary());
        return copy;
    }

    private static final class LockHarness {
        private final ReentrantLock lock = new ReentrantLock();
        private final ThreadLocal<Boolean> held = ThreadLocal.withInitial(() -> false);

        private void lock() {
            lock.lock();
            held.set(true);
        }

        private void unlockIfHeld() {
            if (Boolean.TRUE.equals(held.get())) {
                held.remove();
                lock.unlock();
            }
        }
    }

    private static final class LockAwareTransactionManager implements PlatformTransactionManager {

        private final LockHarness lockHarness;

        private LockAwareTransactionManager(LockHarness lockHarness) {
            this.lockHarness = lockHarness;
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            lockHarness.unlockIfHeld();
        }

        @Override
        public void rollback(TransactionStatus status) {
            lockHarness.unlockIfHeld();
        }
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
