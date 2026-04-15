package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultIndexViewTest {

    @Test
    void hasTerminalBatchResultsRecognizesCompletedBatchStatuses() {
        ResultListItemView result = new ResultListItemView(
                1L,
                2L,
                "s2210001",
                "DONE",
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                1,
                1,
                null);

        assertThat(indexFor(BatchStatus.COMPLETED, result).hasTerminalBatchResults()).isTrue();
        assertThat(indexFor(BatchStatus.COMPLETED_WITH_ERRORS, result).hasTerminalBatchResults()).isTrue();
        assertThat(indexFor(BatchStatus.PRECHECKED, result).hasTerminalBatchResults()).isFalse();
    }

    private ResultIndexView indexFor(BatchStatus status, ResultListItemView result) {
        Batch batch = new Batch();
        batch.setStatus(status);
        return new ResultIndexView(new Assignment(), batch, List.of(result));
    }
}
