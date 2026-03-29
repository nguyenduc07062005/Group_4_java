package com.group4.javagrader.dto;

import java.math.BigDecimal;
import java.util.List;

public class AssignmentDetailDto {
    private final Long id;
    private final Long semesterId;
    private final String semesterCode;
    private final String semesterName;
    private final String title;
    private final String description;
    private final String gradingMode;
    private final BigDecimal totalScore;
    private final BigDecimal plagiarismThreshold;
    private final List<ProblemDetailDto> problems;

    public AssignmentDetailDto(
            Long id,
            Long semesterId,
            String semesterCode,
            String semesterName,
            String title,
            String description,
            String gradingMode,
            BigDecimal totalScore,
            BigDecimal plagiarismThreshold,
            List<ProblemDetailDto> problems) {
        this.id = id;
        this.semesterId = semesterId;
        this.semesterCode = semesterCode;
        this.semesterName = semesterName;
        this.title = title;
        this.description = description;
        this.gradingMode = gradingMode;
        this.totalScore = totalScore;
        this.plagiarismThreshold = plagiarismThreshold;
        this.problems = problems;
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

    public String getDescription() {
        return description;
    }

    public String getGradingMode() {
        return gradingMode;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public BigDecimal getPlagiarismThreshold() {
        return plagiarismThreshold;
    }

    public List<ProblemDetailDto> getProblems() {
        return problems;
    }

    public int getProblemCount() {
        return problems.size();
    }

    public int getTestCaseCount() {
        return problems.stream().mapToInt(ProblemDetailDto::getTestCaseCount).sum();
    }
}
