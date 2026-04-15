package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AutoGradeResult;
import com.group4.javagrader.service.AutoGradeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/assignments/{assignmentId}/auto-grade")
public class AutoGradeController {

    private final AutoGradeService autoGradeService;

    public AutoGradeController(AutoGradeService autoGradeService) {
        this.autoGradeService = autoGradeService;
    }

    @PostMapping
    public String runAutoGrade(
            @PathVariable("assignmentId") Long assignmentId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        AutoGradeResult result = autoGradeService.run(assignmentId, resolveUsername(principal));
        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Auto-grade started. Plagiarism check completed, batch snapshot created, and grading is now running.");
            return "redirect:/assignments/" + assignmentId + "/grading#batch-progress";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Auto-grade failed at step: " + result.getFailedStep() + ". " + result.getErrorMessage());
            return "redirect:/assignments/" + assignmentId + "/grading#snapshot-run";
        }
    }

    private String resolveUsername(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
