package com.group4.javagrader.dto;

import java.math.BigDecimal;

public class AssignmentSummaryDto {
    private final Long id;
    private final Long semesterId;
    private final String semesterCode;
    private final String semesterName;
    private final String title;
    private final String gradingMode;
    private final BigDecimal totalScore;
    private final int problemCount;

    public AssignmentSummaryDto(
            Long id,
            Long semesterId,
            String semesterCode,
            String semesterName,
            String title,
            String gradingMode,
            BigDecimal totalScore,
            int problemCount) {
        this.id = id;
        this.semesterId = semesterId;
        this.semesterCode = semesterCode;
        this.semesterName = semesterName;
        this.title = title;
        this.gradingMode = gradingMode;
        this.totalScore = totalScore;
        this.problemCount = problemCount;
    }

    public Long getId() {
        return id;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public String getTitle() {
        return title;
    }

    public String getGradingMode() {
        return gradingMode;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public int getProblemCount() {
        return problemCount;
    }
}
