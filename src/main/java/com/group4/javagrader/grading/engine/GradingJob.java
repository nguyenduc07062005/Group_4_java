package com.group4.javagrader.grading.engine;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.Submission;

public record GradingJob(
        Assignment assignment,
        Batch batch,
        Submission submission) {
}
