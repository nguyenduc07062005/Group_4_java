package com.group4.javagrader.dto;

import java.time.LocalDate;
import java.util.List;

public class SemesterDetailDto {
    private final Long id;
    private final String code;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<AssignmentSummaryDto> assignments;

    public SemesterDetailDto(
            Long id,
            String code,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            List<AssignmentSummaryDto> assignments) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.assignments = assignments;
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

    public List<AssignmentSummaryDto> getAssignments() {
        return assignments;
    }
}
