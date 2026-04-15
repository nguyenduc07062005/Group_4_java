package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import lombok.Value;

import java.util.List;

@Value
public class BatchProgressView {

    Assignment assignment;
    Batch batch;
    int pendingCount;
    int runningCount;
    int completedCount;
    int successCount;
    int terminalFailureCount;
    List<BatchSubmissionProgressView> submissions;

    public boolean hasActiveRun() {
        return batch.getStatus() == BatchStatus.QUEUED
                || batch.isReadyForGrading()
                || batch.getStatus() == BatchStatus.RUNNING;
    }
}
