package com.group4.javagrader.grading.engine;

import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.storage.GradingArtifactStorageService;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaCoreSubmissionGrader extends AbstractSubmissionGrader {

    public JavaCoreSubmissionGrader(
            SubmissionStorageService submissionStorageService,
            GradingArtifactStorageService artifactStorageService,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            @Value("${app.grading.timeout-millis:3000}") long timeoutMillis) {
        super(submissionStorageService, artifactStorageService, problemRepository, testCaseRepository, timeoutMillis);
    }

    @Override
    protected List<RuleCheckOutcome> evaluateRuleOutcomes(GradingJob job, List<SubmissionFile> files) {
        return List.of();
    }

    @Override
    protected String resolveMainClass(List<SubmissionFile> files) {
        return "Main";
    }
}
