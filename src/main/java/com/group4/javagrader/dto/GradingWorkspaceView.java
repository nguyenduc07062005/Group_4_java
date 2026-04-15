package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import lombok.Value;

@Value
public class GradingWorkspaceView {

    BatchPrecheckView precheck;
    BatchProgressView latestProgress;
    ResultIndexView resultIndex;
    ResultDetailView selectedResultDetail;
    Long selectedResultId;

    public Assignment assignment() {
        return precheck.getAssignment();
    }

    public boolean hasLatestBatch() {
        return precheck.getLatestBatch() != null;
    }

    public boolean hasLatestProgress() {
        return latestProgress != null;
    }

    public boolean hasSelectedResultDetail() {
        return selectedResultDetail != null;
    }

    public boolean hasStoredResults() {
        return resultIndex != null && !resultIndex.getResults().isEmpty();
    }

    public boolean hasExportableOutputs() {
        return resultIndex != null && resultIndex.hasTerminalBatchResults();
    }

    public boolean hasActiveRun() {
        return latestProgress != null && latestProgress.hasActiveRun();
    }

    public boolean canStartLatestBatch() {
        return precheck.getLatestBatch() != null && precheck.getLatestBatch().canStart();
    }
}
