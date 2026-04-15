package com.group4.javagrader.grading.context;

import com.group4.javagrader.entity.GradingMode;

import java.util.List;

public class SubmissionValidationContext {

    private final GradingMode gradingMode;
    private final String submitterName;
    private final List<SubmissionFile> files;

    public SubmissionValidationContext(GradingMode gradingMode, String submitterName, List<SubmissionFile> files) {
        this.gradingMode = gradingMode;
        this.submitterName = submitterName;
        this.files = files;
    }

    public GradingMode getGradingMode() {
        return gradingMode;
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public List<SubmissionFile> getFiles() {
        return files;
    }
}
