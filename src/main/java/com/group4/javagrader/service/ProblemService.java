package com.group4.javagrader.service;

import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import com.group4.javagrader.entity.Problem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProblemService {

    Long create(ProblemForm form);

    void update(Long id, ProblemForm form);

    void delete(Long assignmentId, Long problemId);

    List<Problem> findByAssignmentId(Long assignmentId);

    List<Problem> findTestcaseProblemsByAssignmentId(Long assignmentId);

    Optional<Problem> findById(Long id);

    Optional<Problem> findAssignmentSettingsProblemByAssignmentId(Long assignmentId);

    Optional<Problem> findPrimaryProblemByAssignmentId(Long assignmentId);

    Problem findOrCreatePrimaryProblem(Long assignmentId);

    void ensureDefaultRuntimeProblem(Long assignmentId, InputMode inputMode, OutputNormalizationPolicy assignmentOutputPolicy);

    void syncPrimaryProblemFromAssignment(
            Long assignmentId,
            String title,
            BigDecimal defaultMark,
            InputMode inputMode,
            OutputNormalizationPolicy assignmentOutputPolicy);
}
