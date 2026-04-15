package com.group4.javagrader.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SubmissionUploadSummary {

    private final int totalSubmissions;
    private final int validatedCount;
    private final int rejectedCount;
}
