package com.group4.javagrader.dto;

import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProblemForm {

    @NotNull(message = "Assignment is required.")
    private Long assignmentId;

    @NotBlank(message = "Problem title is required.")
    @Size(max = 150, message = "Problem title must not exceed 150 characters.")
    private String title;

    @NotNull(message = "Maximum score is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum score cannot be negative.")
    private BigDecimal maxScore = BigDecimal.ZERO;

    @NotNull(message = "Input mode must be selected.")
    private InputMode inputMode = InputMode.STDIN;

    @NotNull(message = "Output comparison mode must be selected.")
    private OutputComparisonMode outputComparisonMode = OutputComparisonMode.EXACT;

    public boolean isFileInputMode() {
        return inputMode == InputMode.FILE;
    }

    public void setInputMode(InputMode inputMode) {
        this.inputMode = inputMode;
    }

    public void setOutputComparisonMode(OutputComparisonMode outputComparisonMode) {
        this.outputComparisonMode = outputComparisonMode;
    }
}
