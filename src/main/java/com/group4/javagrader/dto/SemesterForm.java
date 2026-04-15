package com.group4.javagrader.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SemesterForm {

    @NotBlank(message = "Semester code is required.")
    @Size(max = 50, message = "Semester code must not exceed 50 characters.")
    private String code;

    @NotBlank(message = "Semester name is required.")
    @Size(max = 150, message = "Semester name must not exceed 150 characters.")
    private String name;

    @NotNull(message = "Start date is required.")
    private LocalDate startDate;

    @NotNull(message = "End date is required.")
    private LocalDate endDate;

    @AssertTrue(message = "End date must be on or after the start date.")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
