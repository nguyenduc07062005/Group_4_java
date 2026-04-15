package com.group4.javagrader.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseForm {

    @NotNull(message = "Semester must be selected.")
    private Long semesterId;

    @NotBlank(message = "Course code is required.")
    @Size(max = 50, message = "Course code must not exceed 50 characters.")
    private String courseCode;

    @NotBlank(message = "Course name is required.")
    @Size(max = 150, message = "Course name must not exceed 150 characters.")
    private String courseName;

    @NotNull(message = "Week count is required.")
    @Min(value = 0, message = "Week count cannot be negative.")
    @Max(value = 52, message = "Week count cannot exceed 52.")
    private Integer weekCount = 0;

    private boolean createWeeklyAssignments = true;
}
