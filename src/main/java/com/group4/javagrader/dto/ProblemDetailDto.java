package com.group4.javagrader.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProblemDetailDto {
    private final Long id;
    private final Long assignmentId;
    private final int problemOrder;
    private final String title;
    private final BigDecimal maxScore;
    private final String inputMode;
    private final String outputComparisonMode;
    private final List<TestCaseSummaryDto> testCases;

    public ProblemDetailDto(
            Long id,
            Long assignmentId,
            int problemOrder,
            String title,
            BigDecimal maxScore,
            String inputMode,
            String outputComparisonMode,
            List<TestCaseSummaryDto> testCases) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.problemOrder = problemOrder;
        this.title = title;
        this.maxScore = maxScore;
        this.inputMode = inputMode;
        this.outputComparisonMode = outputComparisonMode;
        this.testCases = testCases;
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public int getProblemOrder() {
        return problemOrder;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public String getInputMode() {
        return inputMode;
    }

    public String getOutputComparisonMode() {
        return outputComparisonMode;
    }

    public List<TestCaseSummaryDto> getTestCases() {
        return testCases;
    }

    public int getTestCaseCount() {
        return testCases.size();
    }

    public long getSampleCount() {
        return testCases.stream().filter(TestCaseSummaryDto::isSample).count();
    }
}
