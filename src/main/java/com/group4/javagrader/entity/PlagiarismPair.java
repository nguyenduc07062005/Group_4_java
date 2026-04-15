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
@Table(name = "plagiarism_pairs")
public class PlagiarismPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private PlagiarismReport report;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "left_submission_id", nullable = false)
    private Submission leftSubmission;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "right_submission_id", nullable = false)
    private Submission rightSubmission;

    @Column(name = "text_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal textScore;

    @Column(name = "token_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal tokenScore;

    @Column(name = "ast_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal astScore;

    @Column(name = "final_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "override_decision", length = 20)
    private String overrideDecision;

    @Column(name = "override_note", columnDefinition = "TEXT")
    private String overrideNote;

    @Column(name = "override_by", length = 100)
    private String overrideBy;

    @Column(name = "override_at")
    private LocalDateTime overrideAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isEffectivelyBlocked() {
        if ("ALLOW".equals(overrideDecision)) {
            return false;
        }
        return blocked || "BLOCK".equals(overrideDecision);
    }

    public boolean isOverrideAllowed() {
        return "ALLOW".equals(overrideDecision);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlagiarismReport getReport() {
        return report;
    }

    public void setReport(PlagiarismReport report) {
        this.report = report;
    }

    public Submission getLeftSubmission() {
        return leftSubmission;
    }

    public void setLeftSubmission(Submission leftSubmission) {
        this.leftSubmission = leftSubmission;
    }

    public Submission getRightSubmission() {
        return rightSubmission;
    }

    public void setRightSubmission(Submission rightSubmission) {
        this.rightSubmission = rightSubmission;
    }

    public BigDecimal getTextScore() {
        return textScore;
    }

    public void setTextScore(BigDecimal textScore) {
        this.textScore = textScore;
    }

    public BigDecimal getTokenScore() {
        return tokenScore;
    }

    public void setTokenScore(BigDecimal tokenScore) {
        this.tokenScore = tokenScore;
    }

    public BigDecimal getAstScore() {
        return astScore;
    }

    public void setAstScore(BigDecimal astScore) {
        this.astScore = astScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOverrideDecision() {
        return overrideDecision;
    }

    public void setOverrideDecision(String overrideDecision) {
        this.overrideDecision = overrideDecision;
    }

    public String getOverrideNote() {
        return overrideNote;
    }

    public void setOverrideNote(String overrideNote) {
        this.overrideNote = overrideNote;
    }

    public String getOverrideBy() {
        return overrideBy;
    }

    public void setOverrideBy(String overrideBy) {
        this.overrideBy = overrideBy;
    }

    public LocalDateTime getOverrideAt() {
        return overrideAt;
    }

    public void setOverrideAt(LocalDateTime overrideAt) {
        this.overrideAt = overrideAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
