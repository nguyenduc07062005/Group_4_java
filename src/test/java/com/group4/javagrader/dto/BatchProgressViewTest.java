package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchProgressViewTest {

    @Test
    void hasActiveRunTreatsQueuedAndRunningStatusesAsActive() {
        assertThat(viewFor(BatchStatus.QUEUED).hasActiveRun()).isTrue();
        assertThat(viewFor(BatchStatus.READY_FOR_GRADING).hasActiveRun()).isTrue();
        assertThat(viewFor(BatchStatus.RUNNING).hasActiveRun()).isTrue();
        assertThat(viewFor(BatchStatus.COMPLETED).hasActiveRun()).isFalse();
    }

    private BatchProgressView viewFor(BatchStatus status) {
        Batch batch = new Batch();
        batch.setStatus(status);
        return new BatchProgressView(
                new Assignment(),
                batch,
                0,
                0,
                0,
                0,
                0,
                List.of());
    }
}
