package com.group4.javagrader.controller;

import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final AssignmentService assignmentService;

    public ProblemController(ProblemService problemService, AssignmentService assignmentService) {
        this.problemService = problemService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam("assignmentId") Long assignmentId, RedirectAttributes redirectAttributes) {
        return assignmentService.findById(assignmentId)
                .map(assignment -> redirectToSetupWorkspace(assignment.getId(), null))
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/create")
    public String create(
            @RequestParam("contextAssignmentId") Long contextAssignmentId,
            @Valid @ModelAttribute("problemForm") ProblemForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (form.getAssignmentId() == null || !form.getAssignmentId().equals(contextAssignmentId)) {
            bindingResult.reject("problemForm.assignmentContext", "Assignment context is invalid.");
        }
        form.setAssignmentId(contextAssignmentId);

        if (bindingResult.hasErrors()) {
            return redirectBackToWorkspaceWithProblemForm(form, bindingResult, redirectAttributes);
        }

        try {
            Long problemId = problemService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Problem created successfully. Add test cases next.");
            return redirectToTestcaseLab(form.getAssignmentId(), problemId);
        } catch (DomainException ex) {
            bindingResult.reject("problemForm.invalid", ex.getMessage());
            return redirectBackToWorkspaceWithProblemForm(form, bindingResult, redirectAttributes);
        }
    }

    private String redirectBackToWorkspaceWithProblemForm(
            ProblemForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("problemForm", form);
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "problemForm", bindingResult);
        if (form.getAssignmentId() == null || assignmentService.findById(form.getAssignmentId()).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
            return "redirect:/semesters";
        }
        return redirectToSetupWorkspace(form.getAssignmentId(), null);
    }

    private String redirectToSetupWorkspace(Long assignmentId, Long selectedProblemId) {
        if (selectedProblemId == null) {
            return "redirect:/assignments/" + assignmentId + "#setup-workspace";
        }
        return "redirect:/assignments/" + assignmentId + "?problemId=" + selectedProblemId + "#setup-workspace";
    }

    private String redirectToTestcaseLab(Long assignmentId, Long problemId) {
        return "redirect:/assignments/" + assignmentId + "?problemId=" + problemId + "#testcase-lab";
    }
}
