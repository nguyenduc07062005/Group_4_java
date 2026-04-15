package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.Submission;
import lombok.Value;

import java.util.List;

@Value
public class ResultDetailView {

    Assignment assignment;
    Batch batch;
    Submission submission;
    GradingResult result;
    List<ProblemResultView> problemResults;
    List<OopRuleResultView> oopRuleResults;
}
