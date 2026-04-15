package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.AssignmentStudioView;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.dto.TestCaseImportForm;
import com.group4.javagrader.dto.TestCaseImportPreviewForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.AssignmentStudioService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assignments/{assignmentId}")
public class AssignmentWorkspaceController {

    private final AssignmentStudioService assignmentStudioService;
    private final AssignmentService assignmentService;
    private final ProblemService problemService;
    private final TestCaseService testCaseService;

    public AssignmentWorkspaceController(
            AssignmentStudioService assignmentStudioService,
            AssignmentService assignmentService,
            ProblemService problemService,
            TestCaseService testCaseService) {
        this.assignmentStudioService = assignmentStudioService;
        this.assignmentService = assignmentService;
        this.problemService = problemService;
        this.testCaseService = testCaseService;
    }

    @GetMapping
    public String showWorkspace(
            @PathVariable("assignmentId") Long assignmentId,
            @RequestParam(value = "problemId", required = false) Long selectedProblemId,
            Model model,
            RedirectAttributes redirectAttributes) {
        return populateWorkspace(model, assignmentId, selectedProblemId, redirectAttributes)
                ? "assignment/detail"
                : "redirect:/semesters";
    }

    @GetMapping("/testcases")
    public String openWorkspaceAtTestcases(
            @PathVariable("assignmentId") Long assignmentId,
            RedirectAttributes redirectAttributes) {
        return assignmentStudioService.build(assignmentId, null)
                .map(view -> {
                    Long problemId = view.getSelectedProblem() != null ? view.getSelectedProblem().getId() : null;
                    return problemId != null
                            ? "redirect:/assignments/" + assignmentId + "?problemId=" + problemId + "#testcase-lab"
                            : "redirect:/assignments/" + assignmentId + "#testcase-lab";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/settings")
    public String updateSettings(
            @PathVariable("assignmentId") Long assignmentId,
            @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
            BindingResult bindingResult,
            @RequestParam(value = "problemId", required = false) Long selectedProblemId,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderWorkspaceWithForms(model, assignmentId, selectedProblemId, redirectAttributes);
        }

        try {
            assignmentService.update(assignmentId, form);
            problemService.syncPrimaryProblemFromAssignment(
                    assignmentId,
                    form.getAssignmentName(),
                    form.getDefaultMark(),
                    form.getInputMode(),
                    form.getOutputNormalizationPolicy());
            redirectAttributes.addFlashAttribute("successMessage", "Assignment settings updated.");
            return redirectToSetupWorkspace(assignmentId, selectedProblemId);
        } catch (AssignmentConfigValidationException ex) {
            bindingResult.rejectValue(ex.getFieldName(), ex.getFieldName() + ".invalid", ex.getMessage());
            return renderWorkspaceWithForms(model, assignmentId, selectedProblemId, redirectAttributes);
        } catch (DomainException ex) {
            bindingResult.reject("assignmentForm.invalid", ex.getMessage());
            return renderWorkspaceWithForms(model, assignmentId, selectedProblemId, redirectAttributes);
        }
    }

    @GetMapping("/questions/{problemId}/edit")
    public String editQuestion(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            Model model,
            RedirectAttributes redirectAttributes) {
        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        model.addAttribute("problemForm", toProblemForm(problem));
        model.addAttribute("editingProblemId", problemId);
        return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
    }

    @PostMapping("/questions")
    public String createQuestion(
            @PathVariable("assignmentId") Long assignmentId,
            @Valid @ModelAttribute("problemForm") ProblemForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        form.setAssignmentId(assignmentId);

        if (bindingResult.hasErrors()) {
            return renderWorkspaceWithForms(model, assignmentId, null, redirectAttributes);
        }

        try {
            Long problemId = problemService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Question created. Continue with testcase setup.");
            return redirectToTestcaseLab(assignmentId, problemId);
        } catch (DomainException ex) {
            bindingResult.reject("problemForm.invalid", ex.getMessage());
            return renderWorkspaceWithForms(model, assignmentId, null, redirectAttributes);
        }
    }

    @PostMapping("/questions/{problemId}")
    public String updateQuestion(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @Valid @ModelAttribute("problemForm") ProblemForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        form.setAssignmentId(assignmentId);

        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingProblemId", problemId);
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }

        try {
            problemService.update(problemId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Question updated.");
            return redirectToTestcaseLab(assignmentId, problemId);
        } catch (DomainException ex) {
            bindingResult.reject("problemForm.invalid", ex.getMessage());
            model.addAttribute("editingProblemId", problemId);
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }
    }

    @GetMapping("/questions/{problemId}/testcases/{testCaseId}/edit")
    public String editTestCase(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @PathVariable("testCaseId") Long testCaseId,
            Model model,
            RedirectAttributes redirectAttributes) {
        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        TestCase testCase = loadWorkspaceTestCase(problemId, testCaseId, redirectAttributes);
        if (testCase == null) {
            return "redirect:/semesters";
        }

        model.addAttribute("testCaseForm", toTestCaseForm(testCase));
        model.addAttribute("editingTestCaseId", testCaseId);
        return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
    }

    @PostMapping("/questions/{problemId}/testcases")
    public String createTestCase(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @Valid @ModelAttribute("testCaseForm") TestCaseForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        form.setProblemId(problemId);

        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        if (bindingResult.hasErrors()) {
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }

        try {
            testCaseService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Testcase saved.");
            return redirectToTestcaseLab(assignmentId, problemId);
        } catch (DomainException ex) {
            bindingResult.reject("testCaseForm.invalid", ex.getMessage());
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }
    }

    @PostMapping("/questions/{problemId}/testcases/{testCaseId}")
    public String updateTestCase(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @PathVariable("testCaseId") Long testCaseId,
            @Valid @ModelAttribute("testCaseForm") TestCaseForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        form.setProblemId(problemId);

        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingTestCaseId", testCaseId);
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }

        try {
            testCaseService.update(testCaseId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Testcase updated.");
            return redirectToTestcaseLab(assignmentId, problemId);
        } catch (DomainException ex) {
            bindingResult.reject("testCaseForm.invalid", ex.getMessage());
            model.addAttribute("editingTestCaseId", testCaseId);
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }
    }

    @PostMapping("/questions/{problemId}/testcases/{testCaseId}/inline")
    @ResponseBody
    public ResponseEntity<?> updateTestCaseInline(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @PathVariable("testCaseId") Long testCaseId,
            @Valid @ModelAttribute TestCaseForm form,
            BindingResult bindingResult) {
        form.setProblemId(problemId);

        if (findWorkspaceProblem(problemId, assignmentId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", "Problem not found."));
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", firstFieldError(bindingResult, "Expected output is required.")));
        }

        try {
            testCaseService.update(testCaseId, form);
            TestCase updatedTestCase = testCaseService.findByProblemId(problemId).stream()
                    .filter(testCase -> testCase.getId().equals(testCaseId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Testcase not found."));
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Testcase updated.",
                    "testcase", toInlineTestCasePayload(updatedTestCase)));
        } catch (DomainException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", ex.getMessage()));
        }
    }

    @PostMapping("/questions/{problemId}/testcases/{testCaseId}/delete-inline")
    @ResponseBody
    public ResponseEntity<?> deleteTestCaseInline(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @PathVariable("testCaseId") Long testCaseId) {
        try {
            if (findWorkspaceProblem(problemId, assignmentId).isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "message", "Problem not found."));
            }
            testCaseService.delete(problemId, testCaseId);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Testcase deleted.",
                    "deletedId", testCaseId,
                    "remainingCount", testCaseService.findByProblemId(problemId).size()));
        } catch (DomainException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", ex.getMessage()));
        }
    }

    @PostMapping("/questions/{problemId}/testcases/import")
    public String importTestCases(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @ModelAttribute("testCaseImportForm") TestCaseImportForm importForm,
            Model model,
            RedirectAttributes redirectAttributes) {
        importForm.setProblemId(problemId);

        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        try {
            model.addAttribute("testCaseImportPreviewForm", testCaseService.buildImportPreview(problemId, importForm.getImportFile()));
        } catch (DomainException ex) {
            model.addAttribute("importErrorMessage", ex.getMessage());
        }

        return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
    }

    @PostMapping("/questions/{problemId}/testcases/import/preview")
    public String removeImportedPreviewRow(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @ModelAttribute("testCaseImportPreviewForm") TestCaseImportPreviewForm previewForm,
            @RequestParam("removeIndex") int removeIndex,
            Model model,
            RedirectAttributes redirectAttributes) {
        previewForm.setProblemId(problemId);

        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        if (previewForm.getRows() == null || removeIndex < 0 || removeIndex >= previewForm.getRows().size()) {
            model.addAttribute("importErrorMessage", "The selected preview row was not found.");
        } else {
            previewForm.getRows().remove(removeIndex);
        }

        return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
    }

    @PostMapping("/questions/{problemId}/testcases/import/save")
    public String saveImportedPreview(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @Valid @ModelAttribute("testCaseImportPreviewForm") TestCaseImportPreviewForm previewForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        previewForm.setProblemId(problemId);

        Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
        if (problem == null) {
            return "redirect:/semesters";
        }

        if (previewForm.getRows() == null || previewForm.getRows().isEmpty()) {
            String message = "Import preview is empty.";
            bindingResult.reject("testCaseImportPreviewForm.empty", message);
            model.addAttribute("importErrorMessage", message);
        }

        if (bindingResult.hasErrors()) {
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }

        try {
            long savedCount = testCaseService.saveImportedTestCases(previewForm);
            redirectAttributes.addFlashAttribute("successMessage", savedCount + " imported testcases saved.");
            return redirectToTestcaseLab(assignmentId, problemId);
        } catch (DomainException ex) {
            bindingResult.reject("testCaseImportPreviewForm.invalid", ex.getMessage());
            return renderWorkspaceWithForms(model, assignmentId, problemId, redirectAttributes);
        }
    }

    @PostMapping("/questions/{problemId}/delete")
    public String deleteQuestion(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            RedirectAttributes redirectAttributes) {
        try {
            Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
            if (problem == null) {
                return "redirect:/semesters";
            }
            problemService.delete(assignmentId, problemId);
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted.");
            return "redirect:/assignments/" + assignmentId + "#testcase-lab";
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return redirectToTestcaseLab(assignmentId, problemId);
        }
    }

    @PostMapping("/questions/{problemId}/testcases/{testCaseId}/delete")
    public String deleteTestCase(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("problemId") Long problemId,
            @PathVariable("testCaseId") Long testCaseId,
            RedirectAttributes redirectAttributes) {
        try {
            Problem problem = loadWorkspaceProblem(problemId, assignmentId, redirectAttributes);
            if (problem == null) {
                return "redirect:/semesters";
            }
            testCaseService.delete(problemId, testCaseId);
            redirectAttributes.addFlashAttribute("successMessage", "Testcase deleted.");
            return redirectToTestcaseLab(assignmentId, problemId);
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return redirectToTestcaseLab(assignmentId, problemId);
        }
    }

    private String renderWorkspaceWithForms(
            Model model,
            Long assignmentId,
            Long selectedProblemId,
            RedirectAttributes redirectAttributes) {
        return populateWorkspace(model, assignmentId, selectedProblemId, redirectAttributes)
                ? "assignment/detail"
                : "redirect:/semesters";
    }

    private boolean populateWorkspace(
            Model model,
            Long assignmentId,
            Long selectedProblemId,
            RedirectAttributes redirectAttributes) {
        AssignmentStudioView view = assignmentStudioService.build(assignmentId, selectedProblemId).orElse(null);
        if (view == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
            return false;
        }

        model.addAttribute("assignment", view.getAssignment());
        model.addAttribute("runtimeBlocks", view.getRuntimeBlocks());
        model.addAttribute("selectedProblem", view.getSelectedProblem());
        model.addAttribute("selectedProblemTestCases", view.getSelectedProblemTestCases());
        model.addAttribute("runtimeBlockTestCaseCounts", view.getRuntimeBlockTestCaseCounts());
        model.addAttribute("runtimeBlockCount", view.getRuntimeBlocks().size());
        model.addAttribute("configuredRuntimeBlockCount", view.getConfiguredRuntimeBlockCount());
        model.addAttribute("totalTestCaseCount", view.getTotalTestCaseCount());
        model.addAttribute("submissionCount", view.getSubmissionCount());
        model.addAttribute("allRuntimeBlocksHaveTestCases", view.isAllRuntimeBlocksHaveTestCases());
        model.addAttribute("canRunPlagiarism", view.isCanRunPlagiarism());
        model.addAttribute("canOpenBatchPrecheck", view.isCanOpenBatchPrecheck());
        model.addAttribute("latestBatch", view.getLatestBatch());
        model.addAttribute("canDeleteAssignment", view.isCanDeleteAssignment());
        model.addAttribute("hasDescriptionUpload", view.isHasDescriptionUpload());
        model.addAttribute("hasOopRuleConfigUpload", view.isHasOopRuleConfigUpload());

        if (!model.containsAttribute("assignmentForm")) {
            model.addAttribute("assignmentForm", toAssignmentForm(view.getAssignment()));
        }
        if (!model.containsAttribute("problemForm")) {
            ProblemForm problemForm = new ProblemForm();
            problemForm.setAssignmentId(assignmentId);
            problemForm.setInputMode(view.getAssignment().isOopMode() ? InputMode.FILE : InputMode.STDIN);
            problemForm.setOutputComparisonMode(comparisonModeFromAssignment(view.getAssignment()));
            model.addAttribute("problemForm", problemForm);
        }
        if (!model.containsAttribute("testCaseForm")) {
            TestCaseForm testCaseForm = new TestCaseForm();
            testCaseForm.setProblemId(view.getSelectedProblem() != null ? view.getSelectedProblem().getId() : null);
            model.addAttribute("testCaseForm", testCaseForm);
        }
        if (!model.containsAttribute("testCaseImportForm")) {
            TestCaseImportForm importForm = new TestCaseImportForm();
            importForm.setProblemId(view.getSelectedProblem() != null ? view.getSelectedProblem().getId() : null);
            model.addAttribute("testCaseImportForm", importForm);
        }
        if (!model.containsAttribute("testCaseImportPreviewForm")) {
            TestCaseImportPreviewForm previewForm = new TestCaseImportPreviewForm();
            previewForm.setProblemId(view.getSelectedProblem() != null ? view.getSelectedProblem().getId() : null);
            model.addAttribute("testCaseImportPreviewForm", previewForm);
        }
        return true;
    }

    private AssignmentForm toAssignmentForm(Assignment assignment) {
        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName(assignment.getAssignmentName());
        form.setSemesterId(assignment.getSemester().getId());
        form.setCourseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null);
        form.setGradingMode(assignment.getGradingMode());
        form.setAssignmentType(assignment.getAssignmentType());
        form.setWeekNumber(assignment.getWeekNumber());
        form.setPlagiarismThreshold(assignment.getPlagiarismThreshold().intValue());
        form.setOutputNormalizationPolicy(assignment.getOutputNormalizationPolicy());
        form.setLogicWeight(assignment.getLogicWeight());
        form.setOopWeight(assignment.getOopWeight());
        Optional<Problem> settingsProblem = problemService.findAssignmentSettingsProblemByAssignmentId(assignment.getId());
        InputMode inputMode = settingsProblem
                .map(Problem::getInputMode)
                .orElse(InputMode.STDIN);
        form.setInputMode(inputMode);
        form.setDefaultMark(settingsProblem
                .map(Problem::getMaxScore)
                .orElse(java.math.BigDecimal.valueOf(100)));
        return form;
    }

    private ProblemForm toProblemForm(Problem problem) {
        ProblemForm form = new ProblemForm();
        form.setAssignmentId(problem.getAssignment().getId());
        form.setTitle(problem.getTitle());
        form.setMaxScore(problem.getMaxScore());
        form.setInputMode(problem.getInputMode());
        form.setOutputComparisonMode(problem.getOutputComparisonMode());
        return form;
    }

    private TestCaseForm toTestCaseForm(TestCase testCase) {
        TestCaseForm form = new TestCaseForm();
        form.setProblemId(testCase.getProblem().getId());
        form.setInputData(testCase.getInputData());
        form.setExpectedOutput(testCase.getExpectedOutput());
        form.setWeight(testCase.getWeight());
        return form;
    }

    private Map<String, Object> toInlineTestCasePayload(TestCase testCase) {
        return Map.of(
                "id", testCase.getId(),
                "caseOrder", testCase.getCaseOrder(),
                "weight", testCase.getWeight(),
                "inputData", Optional.ofNullable(testCase.getInputData()).orElse(""),
                "inputPreview", Optional.ofNullable(testCase.getInputData()).filter(value -> !value.isBlank()).map(value -> abbreviate(value, 80)).orElse("(empty)"),
                "expectedOutput", testCase.getExpectedOutput(),
                "expectedOutputPreview", abbreviate(testCase.getExpectedOutput(), 80));
    }

    private String firstFieldError(BindingResult bindingResult, String fallbackMessage) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse(fallbackMessage);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private Problem loadWorkspaceProblem(Long problemId, Long assignmentId, RedirectAttributes redirectAttributes) {
        return findWorkspaceProblem(problemId, assignmentId)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
                    return null;
                });
    }

    private Optional<Problem> findWorkspaceProblem(Long problemId, Long assignmentId) {
        return problemService.findById(problemId)
                .filter(problem -> problem.getAssignment().getId().equals(assignmentId));
    }

    private TestCase loadWorkspaceTestCase(Long problemId, Long testCaseId, RedirectAttributes redirectAttributes) {
        return testCaseService.findByProblemId(problemId).stream()
                .filter(testCase -> testCase.getId().equals(testCaseId))
                .findFirst()
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Testcase not found.");
                    return null;
                });
    }

    private OutputComparisonMode comparisonModeFromAssignment(Assignment assignment) {
        return assignment.getOutputNormalizationPolicy().toComparisonMode();
    }

    private String redirectToSetupWorkspace(Long assignmentId, Long selectedProblemId) {
        if (selectedProblemId == null) {
            return "redirect:/assignments/" + assignmentId + "#setup-workspace";
        }
        return "redirect:/assignments/" + assignmentId + "?problemId=" + selectedProblemId + "#setup-workspace";
    }

    private String redirectToTestcaseLab(Long assignmentId, Long selectedProblemId) {
        if (selectedProblemId == null) {
            return "redirect:/assignments/" + assignmentId + "#testcase-lab";
        }
        return "redirect:/assignments/" + assignmentId + "?problemId=" + selectedProblemId + "#testcase-lab";
    }
}
