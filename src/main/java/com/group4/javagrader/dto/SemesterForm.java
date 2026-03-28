package com.group4.javagrader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SemesterForm {

    @NotBlank(message = "Semester name is required")
    @Size(max = 50, message = "Semester name must not exceed 50 characters")
    private String name;

    public SemesterForm() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}