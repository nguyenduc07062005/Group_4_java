package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class OopRuleResultView {

    String ruleLabel;
    boolean passed;
    String message;
}
