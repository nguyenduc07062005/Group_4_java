package com.group4.javagrader.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
public class ProblemResultView {

    Long id;
    String title;
    Integer problemOrder;
    String status;
    BigDecimal earnedScore;
    BigDecimal maxScore;
    Integer testcasePassedCount;
    Integer testcaseTotalCount;
    String detailSummary;
    List<TestCaseResultView> testCases;
}
