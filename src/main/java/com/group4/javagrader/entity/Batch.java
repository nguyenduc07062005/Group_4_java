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

import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plagiarism_report_id")
    private PlagiarismReport plagiarismReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BatchStatus status;

    @Column(name = "queue_capacity", nullable = false)
    private Integer queueCapacity;

    @Column(name = "worker_count", nullable = false)
    private Integer workerCount;

    @Column(name = "total_submissions", nullable = false)
    private Integer totalSubmissions;

    @Column(name = "gradeable_submission_count", nullable = false)
    private Integer gradeableSubmissionCount;

    @Column(name = "excluded_submission_count", nullable = false)
    private Integer excludedSubmissionCount;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "started_by", length = 100)
    private String startedBy;

    @Column(name = "precheck_summary", columnDefinition = "TEXT")
    private String precheckSummary;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public boolean canStart() {
        return BatchStatus.PRECHECKED == status;
    }

    public boolean isReadyForGrading() {
        return BatchStatus.READY_FOR_GRADING == status;
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

    public PlagiarismReport getPlagiarismReport() {
        return plagiarismReport;
    }

    public void setPlagiarismReport(PlagiarismReport plagiarismReport) {
        this.plagiarismReport = plagiarismReport;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Integer getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(Integer queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public Integer getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
    }

    public Integer getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(Integer totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public Integer getGradeableSubmissionCount() {
        return gradeableSubmissionCount;
    }

    public void setGradeableSubmissionCount(Integer gradeableSubmissionCount) {
        this.gradeableSubmissionCount = gradeableSubmissionCount;
    }

    public Integer getExcludedSubmissionCount() {
        return excludedSubmissionCount;
    }

    public void setExcludedSubmissionCount(Integer excludedSubmissionCount) {
        this.excludedSubmissionCount = excludedSubmissionCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public String getPrecheckSummary() {
        return precheckSummary;
    }

    public void setPrecheckSummary(String precheckSummary) {
        this.precheckSummary = precheckSummary;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(LocalDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
