package com.group4.javagrader.grading.engine;

import com.group4.javagrader.entity.TestCase;

import java.math.BigDecimal;

public record TestCaseOutcome(
        TestCase testCase,
        String status,
        boolean passed,
        BigDecimal earnedScore,
        BigDecimal configuredWeight,
        String expectedOutput,
        String actualOutput,
        String message,
        Long runtimeMillis) {
}
