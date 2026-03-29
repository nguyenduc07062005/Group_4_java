package com.group4.javagrader.dto;

import java.time.LocalDate;

public class SemesterSummaryDto {
    private final Long id;
    private final String code;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int assignmentCount;

    public SemesterSummaryDto(Long id, String code, String name, LocalDate startDate, LocalDate endDate, int assignmentCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.assignmentCount = assignmentCount;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getAssignmentCount() {
        return assignmentCount;
    }
}
