package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AssignmentStudioView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.AssignmentStudioService;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SubmissionService;
import com.group4.javagrader.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AssignmentStudioServiceImpl implements AssignmentStudioService {

    private final AssignmentService assignmentService;
    private final ProblemService problemService;
    private final TestCaseService testCaseService;
    private final SubmissionService submissionService;
    private final BatchService batchService;

    public AssignmentStudioServiceImpl(
            AssignmentService assignmentService,
            ProblemService problemService,
            TestCaseService testCaseService,
            SubmissionService submissionService,
            BatchService batchService) {
        this.assignmentService = assignmentService;
        this.problemService = problemService;
        this.testCaseService = testCaseService;
        this.submissionService = submissionService;
        this.batchService = batchService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentStudioView> build(Long assignmentId, Long selectedProblemId) {
        return assignmentService.findById(assignmentId)
                .map(assignment -> buildView(assignment, selectedProblemId));
    }

    private AssignmentStudioView buildView(Assignment assignment, Long selectedProblemId) {
        List<Problem> runtimeBlocks = problemService.findTestcaseProblemsByAssignmentId(assignment.getId());
        Map<Long, Long> runtimeBlockTestCaseCounts = new LinkedHashMap<>();
        for (Problem runtimeBlock : runtimeBlocks) {
            runtimeBlockTestCaseCounts.put(runtimeBlock.getId(), testCaseService.countByProblemId(runtimeBlock.getId()));
        }

        long configuredRuntimeBlockCount = runtimeBlockTestCaseCounts.values().stream()
                .filter(count -> count > 0)
                .count();
        long totalTestCaseCount = runtimeBlockTestCaseCounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        Problem selectedProblem = resolveSelectedProblem(assignment.getId(), runtimeBlocks, selectedProblemId);
        List<TestCase> selectedProblemTestCases = selectedProblem == null
                ? List.of()
                : testCaseService.findByProblemId(selectedProblem.getId());

        List<Submission> submissions = submissionService.findByAssignmentId(assignment.getId());
        Optional<Batch> latestBatch = batchService.findLatestBatch(assignment.getId());
        boolean allRuntimeBlocksHaveTestCases = !runtimeBlocks.isEmpty()
                && configuredRuntimeBlockCount == runtimeBlocks.size();

        return new AssignmentStudioView(
                assignment,
                runtimeBlocks,
                selectedProblem,
                selectedProblemTestCases,
                runtimeBlockTestCaseCounts,
                configuredRuntimeBlockCount,
                totalTestCaseCount,
                submissions.size(),
                allRuntimeBlocksHaveTestCases,
                !submissions.isEmpty(),
                allRuntimeBlocksHaveTestCases && !submissions.isEmpty(),
                assignmentService.canDelete(assignment.getId()),
                assignment.hasDescriptionUpload(),
                assignment.hasOopRuleConfigUpload(),
                latestBatch.orElse(null));
    }

    private Problem resolveSelectedProblem(Long assignmentId, List<Problem> runtimeBlocks, Long selectedProblemId) {
        if (selectedProblemId != null) {
            Optional<Problem> selected = problemService.findById(selectedProblemId)
                    .filter(problem -> problem.getAssignment().getId().equals(assignmentId));
            if (selected.isPresent()) {
                return selected.get();
            }
        }

        if (!runtimeBlocks.isEmpty()) {
            return runtimeBlocks.get(0);
        }

        return problemService.findPrimaryProblemByAssignmentId(assignmentId).orElse(null);
    }
}
