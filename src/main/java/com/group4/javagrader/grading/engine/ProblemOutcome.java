package com.group4.javagrader.grading.engine;

import com.group4.javagrader.entity.Problem;

import java.math.BigDecimal;
import java.util.List;

public record ProblemOutcome(
        Problem problem,
        String status,
        BigDecimal earnedScore,
        BigDecimal maxScore,
        int testcasePassedCount,
        int testcaseTotalCount,
        String detailSummary,
        List<TestCaseOutcome> testCaseOutcomes) {
}
