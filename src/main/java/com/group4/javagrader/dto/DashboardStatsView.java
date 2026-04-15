package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardStatsView {

    int courseCount;
    int assignmentCount;
    int setupReadyCount;
    int validatedSubmissionCount;
    int blockedSubmissionCount;
    int runningBatchCount;
}
