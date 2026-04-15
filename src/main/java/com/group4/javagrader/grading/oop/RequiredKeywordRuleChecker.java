package com.group4.javagrader.grading.oop;

import com.group4.javagrader.grading.engine.RuleCheckOutcome;

import java.util.List;

public class RequiredKeywordRuleChecker implements OopRuleChecker {

    private final String keyword;
    private final String label;

    public RequiredKeywordRuleChecker(String keyword, String label) {
        this.keyword = keyword;
        this.label = label;
    }

    @Override
    public RuleCheckOutcome check(List<String> sourceTexts) {
        boolean passed = sourceTexts.stream().anyMatch(sourceText -> sourceText.contains(keyword));
        String message = passed
                ? "Detected required keyword '" + keyword + "'."
                : "Required keyword '" + keyword + "' was not found in the submission.";
        return new RuleCheckOutcome(label, passed, message);
    }
}
