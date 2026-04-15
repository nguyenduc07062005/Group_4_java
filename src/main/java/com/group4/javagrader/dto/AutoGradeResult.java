package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.PlagiarismReport;
import lombok.Value;

@Value
public class AutoGradeResult {

    boolean success;
    String failedStep;
    String errorMessage;
    PlagiarismReport plagiarismReport;
    Batch batch;

    public static AutoGradeResult success(PlagiarismReport report, Batch batch) {
        return new AutoGradeResult(true, null, null, report, batch);
    }

    public static AutoGradeResult failure(String failedStep, String errorMessage, PlagiarismReport report, Batch batch) {
        return new AutoGradeResult(false, failedStep, errorMessage, report, batch);
    }
}
