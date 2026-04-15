package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardWeekLoadView {

    String label;
    int assignmentCount;
    int submissionCount;
    int assignmentPercent;
    int submissionPercent;
}
