package com.group4.javagrader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProblemForm {

    @NotBlank(message = "Problem name is required")
    private String problemName;

    @NotNull(message = "Assignment ID is missing")
    private Long assignmentId;

    public ProblemForm() {
    }

    public String getProblemName() {
        return problemName;
    }

    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }
}