package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class AssignmentStudioView {

    Assignment assignment;
    List<Problem> runtimeBlocks;
    Problem selectedProblem;
    List<TestCase> selectedProblemTestCases;
    Map<Long, Long> runtimeBlockTestCaseCounts;
    long configuredRuntimeBlockCount;
    long totalTestCaseCount;
    long submissionCount;
    boolean allRuntimeBlocksHaveTestCases;
    boolean canRunPlagiarism;
    boolean canOpenBatchPrecheck;
    boolean canDeleteAssignment;
    boolean hasDescriptionUpload;
    boolean hasOopRuleConfigUpload;
    Batch latestBatch;
}
