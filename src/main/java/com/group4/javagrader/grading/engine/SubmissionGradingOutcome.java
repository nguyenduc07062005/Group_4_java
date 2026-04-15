package com.group4.javagrader.grading.engine;

import java.math.BigDecimal;
import java.util.List;

public record SubmissionGradingOutcome(
        String status,
        BigDecimal totalScore,
        BigDecimal maxScore,
        BigDecimal logicScore,
        BigDecimal oopScore,
        int testcasePassedCount,
        int testcaseTotalCount,
        String compileLog,
        String executionLog,
        String violationSummary,
        String artifactPath,
        List<ProblemOutcome> problemOutcomes,
        List<RuleCheckOutcome> ruleOutcomes) {
}
