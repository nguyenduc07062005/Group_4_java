package com.group4.javagrader.report;

import com.group4.javagrader.dto.ProblemResultView;
import com.group4.javagrader.dto.ResultDetailView;
import com.group4.javagrader.dto.ResultIndexView;
import com.group4.javagrader.dto.ResultListItemView;
import com.group4.javagrader.dto.TestCaseResultView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.service.ResultService;
import com.group4.javagrader.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    private final ResultService resultService = mock(ResultService.class);
    private final ReportServiceImpl reportService = new ReportServiceImpl(resultService);

    @Test
    void gradebookCsvNeutralizesFormulaLikeSubmitterNames() {
        when(resultService.buildIndex(42L)).thenReturn(Optional.of(new ResultIndexView(
                new Assignment(),
                completedBatch(),
                List.of(resultItem(501L, "=cmd|'/C calc'!A0")))));

        String csv = new String(reportService.exportGradebookCsv(42L), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=cmd|'/C calc'!A0\"");
    }

    @Test
    void detailCsvNeutralizesFormulaLikeResultCells() {
        when(resultService.buildIndex(42L)).thenReturn(Optional.of(new ResultIndexView(
                new Assignment(),
                completedBatch(),
                List.of())));
        when(resultService.buildDetailsForLatestBatch(42L)).thenReturn(Optional.of(List.of(resultDetail(
                "+Problem",
                "-expected",
                "@actual",
                "=message"))));

        String csv = new String(reportService.exportDetailCsv(42L), StandardCharsets.UTF_8);

        assertThat(csv)
                .contains("\"'+Problem\"")
                .contains("\"'-expected\"")
                .contains("\"'@actual\"")
                .contains("\"'=message\"");
        verify(resultService, never()).buildDetail(anyLong(), anyLong());
    }

    @Test
    void gradebookCsvRejectsMissingAssignmentAsResourceNotFound() {
        when(resultService.buildIndex(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.exportGradebookCsv(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void detailCsvRejectsMissingAssignmentAsResourceNotFound() {
        when(resultService.buildIndex(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.exportDetailCsv(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void summaryPdfRejectsMissingAssignmentAsResourceNotFound() {
        when(resultService.buildIndex(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.exportSummaryPdf(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void gradebookCsvRejectsNonTerminalLatestBatchAsWorkflowState() {
        Batch runningBatch = new Batch();
        runningBatch.setStatus(BatchStatus.RUNNING);
        when(resultService.buildIndex(42L)).thenReturn(Optional.of(new ResultIndexView(
                new Assignment(),
                runningBatch,
                List.of())));

        assertThatThrownBy(() -> reportService.exportGradebookCsv(42L))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Reports are only available after the latest batch reaches a terminal state.");
    }

    private Batch completedBatch() {
        Batch batch = new Batch();
        batch.setStatus(BatchStatus.COMPLETED);
        return batch;
    }

    private ResultListItemView resultItem(Long resultId, String submitterName) {
        return new ResultListItemView(
                resultId,
                301L,
                submitterName,
                "DONE",
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                1,
                1,
                null);
    }

    private ResultDetailView resultDetail(
            String problemTitle,
            String expectedOutput,
            String actualOutput,
            String message) {
        Submission submission = new Submission();
        submission.setSubmitterName("s2210301");
        GradingResult gradingResult = new GradingResult();
        ProblemResultView problem = new ProblemResultView(
                901L,
                problemTitle,
                1,
                "DONE",
                BigDecimal.TEN,
                BigDecimal.TEN,
                1,
                1,
                "ok",
                List.of(new TestCaseResultView(
                        1,
                        "PASSED",
                        true,
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        expectedOutput,
                        actualOutput,
                        message,
                        7L)));

        return new ResultDetailView(
                new Assignment(),
                completedBatch(),
                submission,
                gradingResult,
                List.of(problem),
                List.of());
    }
}
