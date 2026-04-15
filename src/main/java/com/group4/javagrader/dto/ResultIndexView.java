package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import lombok.Value;

import java.util.List;

@Value
public class ResultIndexView {

    Assignment assignment;
    Batch latestBatch;
    List<ResultListItemView> results;

    public boolean hasLatestBatch() {
        return latestBatch != null;
    }

    public int resultCount() {
        return results.size();
    }

    public boolean hasTerminalBatchResults() {
        return latestBatch != null
                && (latestBatch.getStatus() == BatchStatus.COMPLETED
                || latestBatch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS)
                && !results.isEmpty();
    }
}
