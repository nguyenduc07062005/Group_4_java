package com.group4.javagrader.grading.plagiarism;

public record NormalizedSubmissionSource(
        Long submissionId,
        String submitterName,
        String normalizedSource) {
}
