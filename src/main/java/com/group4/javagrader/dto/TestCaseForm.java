package com.group4.javagrader.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TestCaseForm {

    @NotBlank(message = "Input data is required")
    private String input;

    @NotBlank(message = "Expected output is required")
    private String expectedOutput;

    @NotNull(message = "Weight is required")
    @Min(value = 1, message = "Minimum weight is 1")
    @Max(value = 100, message = "Maximum weight is 100")
    private Integer weight;

    @NotNull(message = "Problem ID is missing")
    private Long problemId;

    public TestCaseForm() {
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }
}