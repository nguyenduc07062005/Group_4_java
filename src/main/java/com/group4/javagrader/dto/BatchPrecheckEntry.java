package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class BatchPrecheckEntry {

    Long submissionId;
    String submitterName;
    String currentStatus;
    String reason;
}
