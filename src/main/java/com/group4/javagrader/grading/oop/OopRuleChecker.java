package com.group4.javagrader.grading.oop;

import com.group4.javagrader.grading.engine.RuleCheckOutcome;

import java.util.List;

public interface OopRuleChecker {

    RuleCheckOutcome check(List<String> sourceTexts);
}
