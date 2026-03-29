package com.group4.javagrader.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProblemForm {

    @NotNull(message = "Problem order is required.")
    @Positive(message = "Problem order must be positive.")
    private Integer problemOrder;

    @NotBlank(message = "Problem title is required.")
    @Size(max = 150, message = "Problem title must be at most 150 characters.")
    private String title;

    @NotNull(message = "Max score is required.")
    @DecimalMin(value = "0.01", message = "Max score must be greater than 0.")
    @Digits(integer = 5, fraction = 2, message = "Max score format is invalid.")
    private BigDecimal maxScore = new BigDecimal("10.00");

    @NotBlank(message = "Input mode is required.")
    @Pattern(regexp = "STDIN", message = "Input mode must be STDIN.")
    private String inputMode = "STDIN";

    @NotBlank(message = "Output comparison mode is required.")
    @Pattern(regexp = "EXACT", message = "Output comparison mode must be EXACT.")
    private String outputComparisonMode = "EXACT";

    public Integer getProblemOrder() {
        return problemOrder;
    }

    public void setProblemOrder(Integer problemOrder) {
        this.problemOrder = problemOrder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public String getInputMode() {
        return inputMode;
    }

    public void setInputMode(String inputMode) {
        this.inputMode = inputMode;
    }

    public String getOutputComparisonMode() {
        return outputComparisonMode;
    }

    public void setOutputComparisonMode(String outputComparisonMode) {
        this.outputComparisonMode = outputComparisonMode;
    }
}
