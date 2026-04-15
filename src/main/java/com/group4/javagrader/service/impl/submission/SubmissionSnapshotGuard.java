package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class SubmissionSnapshotGuard {

    private static final List<BatchStatus> TERMINAL_BATCH_STATUSES = List.of(
            BatchStatus.COMPLETED,
            BatchStatus.COMPLETED_WITH_ERRORS);

    private final BatchRepository batchRepository;
    private final GradingResultRepository gradingResultRepository;

    public SubmissionSnapshotGuard(
            BatchRepository batchRepository,
            GradingResultRepository gradingResultRepository) {
        this.batchRepository = batchRepository;
        this.gradingResultRepository = gradingResultRepository;
    }

    public void rejectReuploadWhenSubmissionHasGradingSnapshot(Submission submission) {
        if (submission.getId() == null) {
            return;
        }
        if (gradingResultRepository.existsBySubmissionId(submission.getId())) {
            throw new WorkflowStateException(
                    "Submission for "
                            + submission.getSubmitterName()
                            + " is already part of a grading snapshot and cannot be replaced.");
        }
    }

    public void rejectUploadWhenActiveBatchSnapshotWouldBeMutated(
            Long assignmentId,
            Collection<String> submitterNames) {
        for (Batch batch : batchRepository.findByAssignmentIdAndStatusNotInOrderByIdDesc(
                assignmentId,
                TERMINAL_BATCH_STATUSES)) {
            if (touchesFrozenSubmitter(batch, submitterNames)) {
                throw new WorkflowStateException(
                        "Submission upload is blocked while batch snapshot #"
                                + batch.getId()
                                + " is "
                                + batch.getStatus()
                                + ". Wait for the batch to complete before uploading again.");
            }
        }
    }

    private boolean touchesFrozenSubmitter(Batch batch, Collection<String> submitterNames) {
        List<String> normalizedUploadSubmitters = submitterNames.stream()
                .map(this::normalizeSubmitter)
                .toList();

        return gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(batch.getId()).stream()
                .map(result -> result.getSubmission().getSubmitterName())
                .map(this::normalizeSubmitter)
                .anyMatch(normalizedUploadSubmitters::contains);
    }

    private String normalizeSubmitter(String submitterName) {
        return submitterName == null ? "" : submitterName.trim().toLowerCase(Locale.ROOT);
    }
}
