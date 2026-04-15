package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardCourseStatusView {

    String courseCode;
    String courseName;
    int setupCount;
    int intakeCount;
    int plagiarismCount;
    int batchCount;
    int resultsCount;
    int totalCount;

    public int setupPercent() {
        return percentage(setupCount);
    }

    public int intakePercent() {
        return percentage(intakeCount);
    }

    public int plagiarismPercent() {
        return percentage(plagiarismCount);
    }

    public int batchPercent() {
        return percentage(batchCount);
    }

    public int resultsPercent() {
        return percentage(resultsCount);
    }

    private int percentage(int value) {
        if (totalCount <= 0) {
            return 0;
        }
        return (int) Math.round(value * 100.0 / totalCount);
    }
}
