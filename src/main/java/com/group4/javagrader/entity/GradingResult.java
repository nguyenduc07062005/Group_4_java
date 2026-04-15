package com.group4.javagrader.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "grading_results")
public class GradingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private GradingResultStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", nullable = false, length = 30)
    private GradingMode gradingMode;

    @Column(name = "total_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "max_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxScore = BigDecimal.ZERO;

    @Column(name = "logic_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal logicScore = BigDecimal.ZERO;

    @Column(name = "oop_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal oopScore = BigDecimal.ZERO;

    @Column(name = "testcase_passed_count", nullable = false)
    private Integer testcasePassedCount = 0;

    @Column(name = "testcase_total_count", nullable = false)
    private Integer testcaseTotalCount = 0;

    @Column(name = "compile_log", columnDefinition = "TEXT")
    private String compileLog;

    @Column(name = "execution_log", columnDefinition = "LONGTEXT")
    private String executionLog;

    @Column(name = "violation_summary", columnDefinition = "TEXT")
    private String violationSummary;

    @Column(name = "artifact_path", length = 500)
    private String artifactPath;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public boolean isTerminal() {
        return status != GradingResultStatus.PENDING && status != GradingResultStatus.RUNNING;
    }

    public boolean isSuccessful() {
        return status == GradingResultStatus.DONE;
    }

    public boolean isOopMode() {
        return gradingMode == GradingMode.OOP;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public GradingResultStatus getStatus() {
        return status;
    }

    public void setStatus(GradingResultStatus status) {
        this.status = status;
    }

    public GradingMode getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(GradingMode gradingMode) {
        this.gradingMode = gradingMode;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getLogicScore() {
        return logicScore;
    }

    public void setLogicScore(BigDecimal logicScore) {
        this.logicScore = logicScore;
    }

    public BigDecimal getOopScore() {
        return oopScore;
    }

    public void setOopScore(BigDecimal oopScore) {
        this.oopScore = oopScore;
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

    public String getCompileLog() {
        return compileLog;
    }

    public void setCompileLog(String compileLog) {
        this.compileLog = compileLog;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }

    public String getViolationSummary() {
        return violationSummary;
    }

    public void setViolationSummary(String violationSummary) {
        this.violationSummary = violationSummary;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
