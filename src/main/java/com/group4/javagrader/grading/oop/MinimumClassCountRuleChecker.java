package com.group4.javagrader.grading.oop;

import com.group4.javagrader.grading.engine.RuleCheckOutcome;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinimumClassCountRuleChecker implements OopRuleChecker {

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+[A-Za-z_]\\w*");

    private final int expectedClassCount;
    private final String label;

    public MinimumClassCountRuleChecker(int expectedClassCount, String label) {
        this.expectedClassCount = expectedClassCount;
        this.label = label;
    }

    @Override
    public RuleCheckOutcome check(List<String> sourceTexts) {
        int classCount = 0;
        for (String sourceText : sourceTexts) {
            Matcher matcher = CLASS_PATTERN.matcher(sourceText);
            while (matcher.find()) {
                classCount++;
            }
        }

        boolean passed = classCount >= expectedClassCount;
        String message = passed
                ? "Detected " + classCount + " class declarations."
                : "Expected at least " + expectedClassCount + " classes but found " + classCount + ".";
        return new RuleCheckOutcome(label, passed, message);
    }
}
