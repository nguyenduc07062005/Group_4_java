package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.ProblemResultRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.service.ProblemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProblemServiceImpl implements ProblemService {

    private static final String DEFAULT_RUNTIME_TITLE = "Assignment Runtime";
    private static final BigDecimal DEFAULT_RUNTIME_SCORE = BigDecimal.valueOf(100);

    private final ProblemRepository problemRepository;
    private final AssignmentRepository assignmentRepository;
    private final ProblemResultRepository problemResultRepository;
    private final TestCaseRepository testCaseRepository;

    public ProblemServiceImpl(
            ProblemRepository problemRepository,
            AssignmentRepository assignmentRepository,
            ProblemResultRepository problemResultRepository,
            TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.assignmentRepository = assignmentRepository;
        this.problemResultRepository = problemResultRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public Long create(ProblemForm form) {
        Assignment assignment = resolveAssignment(form.getAssignmentId());
        List<Problem> visibleProblems = problemRepository
                .findByAssignmentIdAndInternalDefaultFalseOrderByProblemOrderAsc(form.getAssignmentId());
        Optional<Problem> internalDefaultProblem = problemRepository
                .findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(form.getAssignmentId());

        Problem problem;
        if (visibleProblems.isEmpty()
                && internalDefaultProblem.isPresent()
                && !problemResultRepository.existsByProblemId(internalDefaultProblem.get().getId())) {
            problem = internalDefaultProblem.get();
        } else {
            int nextOrder = problemRepository.findMaxProblemOrderByAssignmentId(form.getAssignmentId()) + 1;
            problem = new Problem();
            problem.setAssignment(assignment);
            problem.setProblemOrder(nextOrder);
        }

        problem.setTitle(form.getTitle().trim());
        problem.setMaxScore(form.getMaxScore());
        problem.setInputMode(form.getInputMode());
        problem.setOutputComparisonMode(form.getOutputComparisonMode());
        problem.setInternalDefault(false);

        return problemRepository.save(problem).getId();
    }

    @Override
    @Transactional
    public void update(Long id, ProblemForm form) {
        Problem problem = problemRepository.findById(id)
                .filter(existingProblem -> !existingProblem.isInternalDefault())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found."));

        if (!problem.getAssignment().getId().equals(form.getAssignmentId())) {
            throw new OwnershipViolationException("Problem does not belong to this assignment.");
        }

        problem.setTitle(form.getTitle().trim());
        problem.setMaxScore(form.getMaxScore());
        problem.setInputMode(form.getInputMode());
        problem.setOutputComparisonMode(form.getOutputComparisonMode());
        problemRepository.save(problem);
    }

    @Override
    @Transactional
    public void delete(Long assignmentId, Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .filter(existingProblem -> !existingProblem.isInternalDefault())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found."));
        if (!problem.getAssignment().getId().equals(assignmentId)) {
            throw new OwnershipViolationException("Problem does not belong to this assignment.");
        }
        if (testCaseRepository.countByProblemId(problemId) > 0) {
            throw new WorkflowStateException("Remove testcases before deleting this question.");
        }
        problemRepository.delete(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Problem> findByAssignmentId(Long assignmentId) {
        return problemRepository.findByAssignmentIdAndInternalDefaultFalseOrderByProblemOrderAsc(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Problem> findTestcaseProblemsByAssignmentId(Long assignmentId) {
        List<Problem> visibleProblems = findByAssignmentId(assignmentId);
        if (!visibleProblems.isEmpty()) {
            return visibleProblems;
        }

        return problemRepository.findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(assignmentId)
                .map(problem -> List.of(problem))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Problem> findById(Long id) {
        return problemRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Problem> findAssignmentSettingsProblemByAssignmentId(Long assignmentId) {
        Optional<Problem> internalDefaultProblem = problemRepository
                .findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(assignmentId);
        if (internalDefaultProblem.isPresent()) {
            return internalDefaultProblem;
        }

        List<Problem> visibleProblems = findByAssignmentId(assignmentId);
        if (visibleProblems.size() == 1) {
            return Optional.of(visibleProblems.get(0));
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Problem> findPrimaryProblemByAssignmentId(Long assignmentId) {
        List<Problem> visibleProblems = findByAssignmentId(assignmentId);
        if (!visibleProblems.isEmpty()) {
            return Optional.of(visibleProblems.get(0));
        }

        return problemRepository.findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(assignmentId);
    }

    @Override
    @Transactional
    public Problem findOrCreatePrimaryProblem(Long assignmentId) {
        return findPrimaryProblemByAssignmentId(assignmentId)
                .orElseGet(() -> createInternalDefaultProblem(resolveAssignment(assignmentId), InputMode.STDIN));
    }

    @Override
    @Transactional
    public void ensureDefaultRuntimeProblem(
            Long assignmentId,
            InputMode inputMode,
            OutputNormalizationPolicy assignmentOutputPolicy) {
        List<Problem> visibleProblems = findByAssignmentId(assignmentId);
        Optional<Problem> internalDefaultProblem = problemRepository
                .findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(assignmentId);

        if (internalDefaultProblem.isPresent()) {
            Problem problem = internalDefaultProblem.get();
            syncInternalDefaultProblem(problem, inputMode, assignmentOutputPolicy);
            problemRepository.save(problem);
            return;
        }

        if (!visibleProblems.isEmpty()) {
            return;
        }

        createInternalDefaultProblem(resolveAssignment(assignmentId), inputMode);
    }

    @Override
    @Transactional
    public void syncPrimaryProblemFromAssignment(
            Long assignmentId,
            String title,
            BigDecimal defaultMark,
            InputMode inputMode,
            OutputNormalizationPolicy assignmentOutputPolicy) {
        Problem problem = resolveAssignmentSettingsProblem(assignmentId, inputMode);

        problem.setTitle(title == null || title.isBlank() ? DEFAULT_RUNTIME_TITLE : title.trim());
        problem.setMaxScore(defaultMark != null ? defaultMark : DEFAULT_RUNTIME_SCORE);
        problem.setInputMode(normalizeInputMode(inputMode));
        problem.setOutputComparisonMode(comparisonModeFromAssignmentPolicy(assignmentOutputPolicy));
        problemRepository.save(problem);
    }

    private Assignment resolveAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
    }

    private Problem createInternalDefaultProblem(Assignment assignment, InputMode inputMode) {
        Problem problem = new Problem();
        problem.setAssignment(assignment);
        problem.setProblemOrder(nextProblemOrder(assignment.getId()));
        problem.setTitle(DEFAULT_RUNTIME_TITLE);
        problem.setMaxScore(DEFAULT_RUNTIME_SCORE);
        problem.setInputMode(normalizeInputMode(inputMode));
        problem.setOutputComparisonMode(comparisonModeFromAssignmentPolicy(assignment.getOutputNormalizationPolicy()));
        problem.setInternalDefault(true);
        return problemRepository.save(problem);
    }

    private void syncInternalDefaultProblem(
            Problem problem,
            InputMode inputMode,
            OutputNormalizationPolicy assignmentOutputPolicy) {
        if (problem.getProblemOrder() == null || problem.getProblemOrder() < 1) {
            problem.setProblemOrder(nextProblemOrder(problem.getAssignment().getId()));
        }
        problem.setTitle(DEFAULT_RUNTIME_TITLE);
        problem.setMaxScore(DEFAULT_RUNTIME_SCORE);
        problem.setInputMode(normalizeInputMode(inputMode));
        problem.setOutputComparisonMode(comparisonModeFromAssignmentPolicy(assignmentOutputPolicy));
        problem.setInternalDefault(true);
    }

    private Problem resolveAssignmentSettingsProblem(Long assignmentId, InputMode inputMode) {
        Optional<Problem> internalDefaultProblem = problemRepository
                .findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(assignmentId);
        if (internalDefaultProblem.isPresent()) {
            return internalDefaultProblem.get();
        }

        List<Problem> visibleProblems = findByAssignmentId(assignmentId);
        if (visibleProblems.size() == 1) {
            return visibleProblems.get(0);
        }

        return createInternalDefaultProblem(resolveAssignment(assignmentId), inputMode);
    }

    private int nextProblemOrder(Long assignmentId) {
        return problemRepository.findMaxProblemOrderByAssignmentId(assignmentId) + 1;
    }

    private InputMode normalizeInputMode(InputMode inputMode) {
        return inputMode == InputMode.FILE ? InputMode.FILE : InputMode.STDIN;
    }

    private OutputComparisonMode comparisonModeFromAssignmentPolicy(OutputNormalizationPolicy assignmentOutputPolicy) {
        return OutputComparisonMode.fromAssignmentPolicy(assignmentOutputPolicy);
    }
}
