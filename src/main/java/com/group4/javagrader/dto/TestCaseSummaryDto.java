package com.group4.javagrader.dto;

public class TestCaseSummaryDto {
    private final Long id;
    private final Long problemId;
    private final int caseOrder;
    private final String inputData;
    private final String expectedOutput;
    private final boolean sample;

    public TestCaseSummaryDto(Long id, Long problemId, int caseOrder, String inputData, String expectedOutput, boolean sample) {
        this.id = id;
        this.problemId = problemId;
        this.caseOrder = caseOrder;
        this.inputData = inputData;
        this.expectedOutput = expectedOutput;
        this.sample = sample;
    }

    public Long getId() {
        return id;
    }

    public Long getProblemId() {
        return problemId;
    }

    public int getCaseOrder() {
        return caseOrder;
    }

    public String getInputData() {
        return inputData;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public boolean isSample() {
        return sample;
    }
}
