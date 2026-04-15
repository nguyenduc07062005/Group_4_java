package com.group4.javagrader.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class TestCaseResultView {

    Integer caseOrder;
    String status;
    boolean passed;
    BigDecimal earnedScore;
    BigDecimal configuredWeight;
    String expectedOutput;
    String actualOutput;
    String message;
    Long runtimeMillis;
}
