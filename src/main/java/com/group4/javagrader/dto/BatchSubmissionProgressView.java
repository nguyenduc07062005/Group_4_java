package com.group4.javagrader.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class BatchSubmissionProgressView {

    Long resultId;
    Long submissionId;
    String submitterName;
    String status;
    BigDecimal totalScore;
    BigDecimal maxScore;
    Integer testcasePassedCount;
    Integer testcaseTotalCount;
    LocalDateTime completedAt;
}
