package com.group4.javagrader.grading.plagiarism;

import java.math.BigDecimal;

public record PlagiarismComparison(
        Long leftSubmissionId,
        Long rightSubmissionId,
        BigDecimal textScore,
        BigDecimal tokenScore,
        BigDecimal astScore,
        BigDecimal finalScore) {
}
