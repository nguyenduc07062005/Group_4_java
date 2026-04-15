package com.group4.javagrader.grading.engine;

public record RuleCheckOutcome(
        String ruleLabel,
        boolean passed,
        String message) {
}
