package com.group4.javagrader.controller;

import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.dto.TestCaseImportForm;
import com.group4.javagrader.dto.TestCaseImportPreviewForm;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/problems/{problemId}/testcases")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final ProblemService problemService;

    public TestCaseController(TestCaseService testCaseService, ProblemService problemService) {
        this.testCaseService = testCaseService;
        this.problemService = problemService;
    }

    @GetMapping("/create")
    public String showCreateForm(@PathVariable("problemId") Long problemId, RedirectAttributes redirectAttributes) {
        return problemService.findById(problemId)
                .map(problem -> redirectToTestcaseLab(problem.getAssignment().getId(), problemId))
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/create")
    public String create(
            @PathVariable("problemId") Long problemId,
            @Valid @ModelAttribute("testCaseForm") TestCaseForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        form.setProblemId(problemId);

        Problem problem = problemService.findById(problemId).orElse(null);
        if (problem == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
            return "redirect:/semesters";
        }

        if (bindingResult.hasErrors()) {
            return redirectBackToWorkspaceWithTestCaseForm(problem, form, bindingResult, redirectAttributes);
        }

        try {
            testCaseService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Test case saved successfully.");
            return redirectToTestcaseLab(problem.getAssignment().getId(), problemId);
        } catch (DomainException ex) {
            bindingResult.reject("testCaseForm.invalid", ex.getMessage());
            return redirectBackToWorkspaceWithTestCaseForm(problem, form, bindingResult, redirectAttributes);
        }
    }

    @PostMapping("/import")
    public String importTestCases(
            @PathVariable("problemId") Long problemId,
            @ModelAttribute("testCaseImportForm") TestCaseImportForm importForm,
            RedirectAttributes redirectAttributes) {
        importForm.setProblemId(problemId);

        Problem problem = problemService.findById(problemId).orElse(null);
        if (problem == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
            return "redirect:/semesters";
        }

        try {
            redirectAttributes.addFlashAttribute("testCaseImportPreviewForm", testCaseService.buildImportPreview(problemId, importForm.getImportFile()));
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("importErrorMessage", ex.getMessage());
        }

        return redirectToTestcaseLab(problem.getAssignment().getId(), problemId);
    }

    @PostMapping("/import/preview")
    public String removeImportedPreviewRow(
            @PathVariable("problemId") Long problemId,
            @ModelAttribute("testCaseImportPreviewForm") TestCaseImportPreviewForm previewForm,
            @RequestParam("removeIndex") int removeIndex,
            RedirectAttributes redirectAttributes) {
        previewForm.setProblemId(problemId);

        Problem problem = problemService.findById(problemId).orElse(null);
        if (problem == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
            return "redirect:/semesters";
        }

        if (previewForm.getRows() == null || removeIndex < 0 || removeIndex >= previewForm.getRows().size()) {
            redirectAttributes.addFlashAttribute("importErrorMessage", "The selected preview row was not found.");
        } else {
            previewForm.getRows().remove(removeIndex);
        }

        redirectAttributes.addFlashAttribute("testCaseImportPreviewForm", previewForm);
        return redirectToTestcaseLab(problem.getAssignment().getId(), problemId);
    }

    @PostMapping("/import/save")
    public String saveImportedPreview(
            @PathVariable("problemId") Long problemId,
            @Valid @ModelAttribute("testCaseImportPreviewForm") TestCaseImportPreviewForm previewForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        previewForm.setProblemId(problemId);

        Problem problem = problemService.findById(problemId).orElse(null);
        if (problem == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Problem not found.");
            return "redirect:/semesters";
        }

        if (previewForm.getRows() == null || previewForm.getRows().isEmpty()) {
            String message = "Import preview is empty.";
            bindingResult.reject("testCaseImportPreviewForm.empty", message);
            redirectAttributes.addFlashAttribute("importErrorMessage", message);
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("testCaseImportPreviewForm", previewForm);
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "testCaseImportPreviewForm", bindingResult);
            return redirectToTestcaseLab(problem.getAssignment().getId(), problemId);
        }

        try {
            long savedCount = testCaseService.saveImportedTestCases(previewForm);
            redirectAttributes.addFlashAttribute("successMessage", savedCount + " imported test cases saved successfully.");
            return redirectToTestcaseLab(problem.getAssignment().getId(), problemId);
        } catch (DomainException ex) {
            bindingResult.reject("testCaseImportPreviewForm.invalid", ex.getMessage());
            redirectAttributes.addFlashAttribute("testCaseImportPreviewForm", previewForm);
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "testCaseImportPreviewForm", bindingResult);
            return redirectToTestcaseLab(problem.getAssignment().getId(), problemId);
        }
    }

    private String redirectBackToWorkspaceWithTestCaseForm(
            Problem problem,
            TestCaseForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("testCaseForm", form);
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "testCaseForm", bindingResult);
        return redirectToTestcaseLab(problem.getAssignment().getId(), problem.getId());
    }

    private String redirectToTestcaseLab(Long assignmentId, Long problemId) {
        return "redirect:/assignments/" + assignmentId + "?problemId=" + problemId + "#testcase-lab";
    }
}
