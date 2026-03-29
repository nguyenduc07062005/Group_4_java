package com.group4.javagrader.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

public class AssignmentForm {

    @NotBlank(message = "Assignment name must not be empty.")
    private String assignmentName;

    @NotBlank(message = "Grading mode must be selected.")
    @Pattern(regexp = "JAVA_CORE|OOP", message = "Grading mode must be JAVA_CORE or OOP.")
    private String gradingMode = "JAVA_CORE";

    @NotNull(message = "Plagiarism threshold is required.")
    @Min(value = 0, message = "Threshold cannot be negative.")
    @Max(value = 100, message = "Threshold cannot exceed 100.")
    private Integer plagiarismThreshold = 80;

    @NotBlank(message = "Output normalization policy must be selected.")
    @Pattern(
            regexp = "STRICT|TRIM_ALL|IGNORE_WHITESPACE",
            message = "Output normalization policy is invalid.")
    private String outputNormalizationPolicy = "STRICT";

    @Min(value = 0, message = "Weight cannot be negative.")
    @Max(value = 100, message = "Weight cannot exceed 100.")
    private Integer logicWeight = 50;

    @Min(value = 0, message = "Weight cannot be negative.")
    @Max(value = 100, message = "Weight cannot exceed 100.")
    private Integer oopWeight = 50;

    private MultipartFile descriptionFile;

    private MultipartFile oopRuleConfig;

    @AssertTrue(message = "Logic weight is required when grading mode is OOP.")
    public boolean isLogicWeightPresentWhenOop() {
        return !isOopMode() || logicWeight != null;
    }

    @AssertTrue(message = "OOP weight is required when grading mode is OOP.")
    public boolean isOopWeightPresentWhenOop() {
        return !isOopMode() || oopWeight != null;
    }

    @AssertTrue(message = "Logic weight and OOP weight must sum to 100.")
    public boolean isWeightDistributionValid() {
        if (!isOopMode() || logicWeight == null || oopWeight == null) {
            return true;
        }
        return logicWeight + oopWeight == 100;
    }

    private boolean isOopMode() {
        return "OOP".equals(gradingMode);
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public String getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(String gradingMode) {
        this.gradingMode = gradingMode;
    }

    public Integer getPlagiarismThreshold() {
        return plagiarismThreshold;
    }

    public void setPlagiarismThreshold(Integer plagiarismThreshold) {
        this.plagiarismThreshold = plagiarismThreshold;
    }

    public String getOutputNormalizationPolicy() {
        return outputNormalizationPolicy;
    }

    public void setOutputNormalizationPolicy(String outputNormalizationPolicy) {
        this.outputNormalizationPolicy = outputNormalizationPolicy;
    }

    public Integer getLogicWeight() {
        return logicWeight;
    }

    public void setLogicWeight(Integer logicWeight) {
        this.logicWeight = logicWeight;
    }

    public Integer getOopWeight() {
        return oopWeight;
    }

    public void setOopWeight(Integer oopWeight) {
        this.oopWeight = oopWeight;
    }

    public MultipartFile getDescriptionFile() {
        return descriptionFile;
    }

    public void setDescriptionFile(MultipartFile descriptionFile) {
        this.descriptionFile = descriptionFile;
    }

    public MultipartFile getOopRuleConfig() {
        return oopRuleConfig;
    }

    public void setOopRuleConfig(MultipartFile oopRuleConfig) {
        this.oopRuleConfig = oopRuleConfig;
    }
}
