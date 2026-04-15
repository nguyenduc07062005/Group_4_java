package com.group4.javagrader.grading.engine;

import com.group4.javagrader.entity.GradingMode;
import org.springframework.stereotype.Component;

@Component
public class SubmissionGraderFactory {

    private final JavaCoreSubmissionGrader javaCoreSubmissionGrader;
    private final OopSubmissionGrader oopSubmissionGrader;

    public SubmissionGraderFactory(
            JavaCoreSubmissionGrader javaCoreSubmissionGrader,
            OopSubmissionGrader oopSubmissionGrader) {
        this.javaCoreSubmissionGrader = javaCoreSubmissionGrader;
        this.oopSubmissionGrader = oopSubmissionGrader;
    }

    public SubmissionGrader resolve(GradingMode gradingMode) {
        if (gradingMode == GradingMode.OOP) {
            return oopSubmissionGrader;
        }
        return javaCoreSubmissionGrader;
    }
}
