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
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 150)
    private String assignmentName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", nullable = false, length = 30)
    private GradingMode gradingMode;

    @Column(name = "plagiarism_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal plagiarismThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_normalization_policy", nullable = false, length = 50)
    private OutputNormalizationPolicy outputNormalizationPolicy;

    @Column(name = "logic_weight", nullable = false)
    private Integer logicWeight;

    @Column(name = "oop_weight", nullable = false)
    private Integer oopWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private AssignmentType assignmentType;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "description_file_name", length = 255)
    private String descriptionFileName;

    @Column(name = "description_file_content_type", length = 100)
    private String descriptionFileContentType;

    @Column(name = "oop_rule_config_file_name", length = 255)
    private String oopRuleConfigFileName;

    @Column(name = "oop_rule_config_content_type", length = 100)
    private String oopRuleConfigContentType;

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

    public boolean isJavaCoreMode() {
        return gradingMode == GradingMode.JAVA_CORE;
    }

    public boolean isOopMode() {
        return gradingMode == GradingMode.OOP;
    }

    public boolean isWeeklyAssignmentType() {
        return assignmentType == AssignmentType.WEEKLY;
    }

    public boolean isCustomAssignmentType() {
        return assignmentType == AssignmentType.CUSTOM;
    }

    public boolean isIntroAssignmentType() {
        return assignmentType == AssignmentType.INTRO;
    }

    public boolean isDefaultAssignmentType() {
        return assignmentType == AssignmentType.DEFAULT;
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

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
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

    public GradingMode getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(GradingMode gradingMode) {
        this.gradingMode = gradingMode;
    }

    public BigDecimal getPlagiarismThreshold() {
        return plagiarismThreshold;
    }

    public void setPlagiarismThreshold(BigDecimal plagiarismThreshold) {
        this.plagiarismThreshold = plagiarismThreshold;
    }

    public OutputNormalizationPolicy getOutputNormalizationPolicy() {
        return outputNormalizationPolicy;
    }

    public void setOutputNormalizationPolicy(OutputNormalizationPolicy outputNormalizationPolicy) {
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

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
