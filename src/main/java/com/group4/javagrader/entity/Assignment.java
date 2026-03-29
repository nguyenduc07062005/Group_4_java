package com.group4.javagrader.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "title", nullable = false, length = 150)
    private String assignmentName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "grading_mode", nullable = false, length = 30)
    private String gradingMode;

    @Column(name = "plagiarism_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal plagiarismThreshold;

    @Column(name = "output_normalization_policy", nullable = false, length = 50)
    private String outputNormalizationPolicy;

    @Column(name = "logic_weight", nullable = false)
    private Integer logicWeight;

    @Column(name = "oop_weight", nullable = false)
    private Integer oopWeight;

    @Column(name = "description_file_name", length = 255)
    private String descriptionFileName;

    @Column(name = "description_file_content_type", length = 100)
    private String descriptionFileContentType;

    @Lob
    @Column(name = "description_file_data", columnDefinition = "LONGBLOB")
    private byte[] descriptionFileData;

    @Column(name = "oop_rule_config_file_name", length = 255)
    private String oopRuleConfigFileName;

    @Column(name = "oop_rule_config_content_type", length = 100)
    private String oopRuleConfigContentType;

    @Lob
    @Column(name = "oop_rule_config_data", columnDefinition = "LONGBLOB")
    private byte[] oopRuleConfigData;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public boolean hasDescriptionUpload() {
        return descriptionFileName != null && !descriptionFileName.isBlank();
    }

    public boolean hasOopRuleConfigUpload() {
        return oopRuleConfigFileName != null && !oopRuleConfigFileName.isBlank();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(String gradingMode) {
        this.gradingMode = gradingMode;
    }

    public BigDecimal getPlagiarismThreshold() {
        return plagiarismThreshold;
    }

    public void setPlagiarismThreshold(BigDecimal plagiarismThreshold) {
        this.plagiarismThreshold = plagiarismThreshold;
    }

    public String getOutputNormalizationPolicy() {
        return outputNormalizationPolicy;
    }

    public void setOutputNormalizationPolicy(String outputNormalizationPolicy) {
        this.outputNormalizationPolicy = outputNormalizationPolicy;
    }

    public Integer getLogicWeight() {
        return logicWeight;
    }

    public void setLogicWeight(Integer logicWeight) {
        this.logicWeight = logicWeight;
    }

    public Integer getOopWeight() {
        return oopWeight;
    }

    public void setOopWeight(Integer oopWeight) {
        this.oopWeight = oopWeight;
    }

    public String getDescriptionFileName() {
        return descriptionFileName;
    }

    public void setDescriptionFileName(String descriptionFileName) {
        this.descriptionFileName = descriptionFileName;
    }

    public String getDescriptionFileContentType() {
        return descriptionFileContentType;
    }

    public void setDescriptionFileContentType(String descriptionFileContentType) {
        this.descriptionFileContentType = descriptionFileContentType;
    }

    public byte[] getDescriptionFileData() {
        return descriptionFileData;
    }

    public void setDescriptionFileData(byte[] descriptionFileData) {
        this.descriptionFileData = descriptionFileData;
    }

    public String getOopRuleConfigFileName() {
        return oopRuleConfigFileName;
    }

    public void setOopRuleConfigFileName(String oopRuleConfigFileName) {
        this.oopRuleConfigFileName = oopRuleConfigFileName;
    }

    public String getOopRuleConfigContentType() {
        return oopRuleConfigContentType;
    }

    public void setOopRuleConfigContentType(String oopRuleConfigContentType) {
        this.oopRuleConfigContentType = oopRuleConfigContentType;
    }

    public byte[] getOopRuleConfigData() {
        return oopRuleConfigData;
    }

    public void setOopRuleConfigData(byte[] oopRuleConfigData) {
        this.oopRuleConfigData = oopRuleConfigData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
