package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.PlagiarismReport;
import lombok.Value;

import java.util.List;

@Value
public class BatchPrecheckView {

    Assignment assignment;
    PlagiarismReport latestReport;
    List<BatchPrecheckEntry> gradeableEntries;
    List<BatchPrecheckEntry> excludedEntries;
    Batch latestBatch;
    int queueCapacity;
    int workerCount;

    public int totalEntries() {
        return gradeableEntries.size() + excludedEntries.size();
    }

    public boolean hasCompletedReport() {
        return latestReport != null && latestReport.isCompleted();
    }

    public boolean canCreateBatch() {
        return hasCompletedReport() && !gradeableEntries.isEmpty();
    }

    public boolean hasLatestBatch() {
        return latestBatch != null;
    }

    public boolean hasStartedBatch() {
        return latestBatch != null && !latestBatch.canStart();
    }
}
