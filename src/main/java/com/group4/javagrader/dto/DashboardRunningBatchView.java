package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardRunningBatchView {

    String assignmentName;
    String courseLabel;
    String statusLabel;
    int progressPercent;
    String progressLabel;
    String href;
}
