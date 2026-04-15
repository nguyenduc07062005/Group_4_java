package com.group4.javagrader.grading.engine;

import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.oop.OopRuleChecker;
import com.group4.javagrader.grading.oop.OopRuleCheckerFactory;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.storage.GradingArtifactStorageService;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OopSubmissionGrader extends AbstractSubmissionGrader {

    private final OopRuleCheckerFactory oopRuleCheckerFactory;

    public OopSubmissionGrader(
            SubmissionStorageService submissionStorageService,
            GradingArtifactStorageService artifactStorageService,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            OopRuleCheckerFactory oopRuleCheckerFactory,
            @Value("${app.grading.timeout-millis:3000}") long timeoutMillis) {
        super(submissionStorageService, artifactStorageService, problemRepository, testCaseRepository, timeoutMillis);
        this.oopRuleCheckerFactory = oopRuleCheckerFactory;
    }

    @Override
    protected List<RuleCheckOutcome> evaluateRuleOutcomes(GradingJob job, List<SubmissionFile> files) {
        List<String> sourceTexts = files.stream()
                .filter(file -> file.relativePath().endsWith(".java"))
                .map(file -> new String(file.content(), StandardCharsets.UTF_8))
                .toList();

        List<RuleCheckOutcome> outcomes = new ArrayList<>();
        for (OopRuleChecker checker : oopRuleCheckerFactory.create(job.assignment())) {
            outcomes.add(checker.check(sourceTexts));
        }
        return outcomes;
    }

    @Override
    protected String resolveMainClass(List<SubmissionFile> files) {
        for (SubmissionFile file : files) {
            if (!file.relativePath().endsWith(".java")) {
                continue;
            }

            String source = new String(file.content(), StandardCharsets.UTF_8);
            if (!containsMainMethod(source)) {
                continue;
            }

            String className = determineClassName(source);
            if (className == null) {
                continue;
            }

            String packageName = determinePackageName(source);
            return packageName.isBlank() ? className : packageName + '.' + className;
        }
        return null;
    }
}
