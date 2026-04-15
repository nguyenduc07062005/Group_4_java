package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardTaskRowView {

    String assignmentName;
    String courseLabel;
    String weekLabel;
    String stageLabel;
    String issueLabel;
    int submissionCount;
    String updatedLabel;
    String tone;
    String actionLabel;
    String href;
}
