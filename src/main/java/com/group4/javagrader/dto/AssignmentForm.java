package com.group4.javagrader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssignmentForm {

    @NotBlank(message = "Assignment name is required")
    private String assignmentName;

    @NotNull(message = "Please select a semester")
    private Long semesterId;

    public AssignmentForm() {
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }
}