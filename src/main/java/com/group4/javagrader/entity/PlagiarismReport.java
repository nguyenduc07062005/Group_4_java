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
@Table(name = "plagiarism_reports")
public class PlagiarismReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal threshold;

    @Column(name = "strategy_summary", columnDefinition = "TEXT")
    private String strategySummary;

    @Column(name = "total_submissions", nullable = false)
    private Integer totalSubmissions;

    @Column(name = "flagged_pair_count", nullable = false)
    private Integer flaggedPairCount;

    @Column(name = "blocked_submission_count", nullable = false)
    private Integer blockedSubmissionCount;

    @Column(name = "run_by", length = 100)
    private String runBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public String getStrategySummary() {
        return strategySummary;
    }

    public void setStrategySummary(String strategySummary) {
        this.strategySummary = strategySummary;
    }

    public Integer getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(Integer totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public Integer getFlaggedPairCount() {
        return flaggedPairCount;
    }

    public void setFlaggedPairCount(Integer flaggedPairCount) {
        this.flaggedPairCount = flaggedPairCount;
    }

    public Integer getBlockedSubmissionCount() {
        return blockedSubmissionCount;
    }

    public void setBlockedSubmissionCount(Integer blockedSubmissionCount) {
        this.blockedSubmissionCount = blockedSubmissionCount;
    }

    public String getRunBy() {
        return runBy;
    }

    public void setRunBy(String runBy) {
        this.runBy = runBy;
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
