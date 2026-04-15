package com.group4.javagrader.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "problem_results")
public class ProblemResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "grading_result_id", nullable = false)
    private GradingResult gradingResult;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "earned_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal earnedScore = BigDecimal.ZERO;

    @Column(name = "max_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxScore = BigDecimal.ZERO;

    @Column(name = "testcase_passed_count", nullable = false)
    private Integer testcasePassedCount = 0;

    @Column(name = "testcase_total_count", nullable = false)
    private Integer testcaseTotalCount = 0;

    @Column(name = "detail_summary", columnDefinition = "TEXT")
    private String detailSummary;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GradingResult getGradingResult() {
        return gradingResult;
    }

    public void setGradingResult(GradingResult gradingResult) {
        this.gradingResult = gradingResult;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getEarnedScore() {
        return earnedScore;
    }

    public void setEarnedScore(BigDecimal earnedScore) {
        this.earnedScore = earnedScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getTestcasePassedCount() {
        return testcasePassedCount;
    }

    public void setTestcasePassedCount(Integer testcasePassedCount) {
        this.testcasePassedCount = testcasePassedCount;
    }

    public Integer getTestcaseTotalCount() {
        return testcaseTotalCount;
    }

    public void setTestcaseTotalCount(Integer testcaseTotalCount) {
        this.testcaseTotalCount = testcaseTotalCount;
    }

    public String getDetailSummary() {
        return detailSummary;
    }

    public void setDetailSummary(String detailSummary) {
        this.detailSummary = detailSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
