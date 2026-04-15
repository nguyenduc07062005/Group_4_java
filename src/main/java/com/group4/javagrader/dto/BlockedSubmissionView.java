package com.group4.javagrader.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class BlockedSubmissionView {

    Long submissionId;
    String submitterName;
    String matchedWith;
    BigDecimal score;
    String reason;
}
