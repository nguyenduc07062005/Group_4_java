package com.group4.javagrader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TestCaseForm {

    @NotNull(message = "Case order is required.")
    @Positive(message = "Case order must be positive.")
    private Integer caseOrder;

    @NotBlank(message = "Input data is required.")
    private String inputData;

    @NotBlank(message = "Expected output is required.")
    private String expectedOutput;

    private boolean sample;

    public Integer getCaseOrder() {
        return caseOrder;
    }

    public void setCaseOrder(Integer caseOrder) {
        this.caseOrder = caseOrder;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public boolean isSample() {
        return sample;
    }

    public void setSample(boolean sample) {
        this.sample = sample;
    }
}
