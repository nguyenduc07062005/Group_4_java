package com.group4.javagrader.dto;

import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
public class AssignmentForm {

    @NotBlank(message = "Assignment name must not be empty.")
    private String assignmentName;

    private Long semesterId;

    private Long courseId;

    @NotNull(message = "Grading mode must be selected.")
    private GradingMode gradingMode = GradingMode.JAVA_CORE;

    @NotNull(message = "Plagiarism threshold is required.")
    @Min(value = 0, message = "Threshold cannot be negative.")
    @Max(value = 100, message = "Threshold cannot exceed 100.")
    private Integer plagiarismThreshold = 80;

    @NotNull(message = "Output normalization policy must be selected.")
    private OutputNormalizationPolicy outputNormalizationPolicy = OutputNormalizationPolicy.STRICT;

    @NotNull(message = "Input mode must be selected.")
    private InputMode inputMode = InputMode.STDIN;

    @NotNull(message = "Default mark is required.")
    @jakarta.validation.constraints.DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Default mark cannot be negative.")
    private BigDecimal defaultMark = BigDecimal.valueOf(100);

    @NotNull(message = "Assignment type must be selected.")
    private AssignmentType assignmentType = AssignmentType.CUSTOM;

    @Min(value = 1, message = "Week number must be at least 1.")
    @Max(value = 52, message = "Week number cannot exceed 52.")
    private Integer weekNumber;

    @Min(value = 0, message = "Weight cannot be negative.")
    @Max(value = 100, message = "Weight cannot exceed 100.")
    private Integer logicWeight = 50;

    @Min(value = 0, message = "Weight cannot be negative.")
    @Max(value = 100, message = "Weight cannot exceed 100.")
    private Integer oopWeight = 50;

    private MultipartFile descriptionFile;

    private MultipartFile oopRuleConfig;

    @AssertTrue(message = "Course or semester must be selected.")
    public boolean isCourseOrSemesterPresent() {
        return courseId != null || semesterId != null;
    }

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

    @AssertTrue(message = "Week number is required when assignment type is WEEKLY.")
    public boolean isWeekNumberPresentWhenWeekly() {
        return !isWeeklyAssignmentType() || weekNumber != null;
    }

    public boolean isOopMode() {
        return gradingMode == GradingMode.OOP;
    }

    public boolean isJavaCoreMode() {
        return gradingMode == GradingMode.JAVA_CORE;
    }

    public boolean isWeeklyAssignmentType() {
        return assignmentType == AssignmentType.WEEKLY;
    }

    public boolean isFileInputMode() {
        return inputMode == InputMode.FILE;
    }
}
