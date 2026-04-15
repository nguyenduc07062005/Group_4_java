package com.group4.javagrader.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TestCaseImportRowForm {

    @NotBlank(message = "Source name is required.")
    private String sourceName;

    private String inputData = "";

    @NotBlank(message = "Expected output is required.")
    private String expectedOutput;

    @NotNull(message = "Weight is required.")
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0.")
    private BigDecimal weight = BigDecimal.ONE;
}
