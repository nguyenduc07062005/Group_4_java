package com.group4.javagrader.grading.plagiarism;

import java.math.BigDecimal;

public interface PlagiarismStrategy {

    String getCode();

    BigDecimal getWeight();

    BigDecimal compare(NormalizedSubmissionSource left, NormalizedSubmissionSource right);
}
