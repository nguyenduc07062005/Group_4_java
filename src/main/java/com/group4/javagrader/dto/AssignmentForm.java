package com.group4.javagrader.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class AssignmentForm {

    @NotNull(message = "Semester is required.")
    private Long semesterId;

    @NotBlank(message = "Assignment title is required.")
    @Size(max = 150, message = "Assignment title must be at most 150 characters.")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters.")
    private String description;

    @NotBlank(message = "Grading mode is required.")
    @Pattern(regexp = "JAVA_CORE|OOP", message = "Grading mode must be JAVA_CORE or OOP.")
    private String gradingMode = "JAVA_CORE";

    @NotNull(message = "Total score is required.")
    @DecimalMin(value = "1.00", message = "Total score must be at least 1.")
    @Digits(integer = 5, fraction = 2, message = "Total score format is invalid.")
    private BigDecimal totalScore = new BigDecimal("100.00");

    @NotNull(message = "Plagiarism threshold is required.")
    @DecimalMin(value = "0.00", message = "Threshold cannot be negative.")
    @DecimalMax(value = "100.00", message = "Threshold cannot exceed 100.")
    @Digits(integer = 5, fraction = 2, message = "Threshold format is invalid.")
    private BigDecimal plagiarismThreshold = new BigDecimal("80.00");

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(String gradingMode) {
        this.gradingMode = gradingMode;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getPlagiarismThreshold() {
        return plagiarismThreshold;
    }

    public void setPlagiarismThreshold(BigDecimal plagiarismThreshold) {
        this.plagiarismThreshold = plagiarismThreshold;
    }
}
