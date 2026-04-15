package com.group4.javagrader.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class ResultListItemView {

    Long resultId;
    Long submissionId;
    String submitterName;
    String status;
    BigDecimal totalScore;
    BigDecimal maxScore;
    BigDecimal logicScore;
    BigDecimal oopScore;
    Integer testcasePassedCount;
    Integer testcaseTotalCount;
    LocalDateTime completedAt;
}
