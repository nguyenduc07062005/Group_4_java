package com.group4.javagrader.controller;

import com.group4.javagrader.service.GradingWorkspaceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assignments/{assignmentId}/grading")
public class GradingWorkspaceController {

    private final GradingWorkspaceService gradingWorkspaceService;

    public GradingWorkspaceController(GradingWorkspaceService gradingWorkspaceService) {
        this.gradingWorkspaceService = gradingWorkspaceService;
    }

    @GetMapping
    public String showWorkspace(
            @PathVariable("assignmentId") Long assignmentId,
            @RequestParam(value = "resultId", required = false) Long resultId,
            Model model,
            RedirectAttributes redirectAttributes) {
        return gradingWorkspaceService.build(assignmentId, resultId)
                .map(workspace -> {
                    model.addAttribute("gradingWorkspace", workspace);
                    if (resultId != null && workspace.getSelectedResultId() == null) {
                        model.addAttribute("warningMessage", "The selected result is no longer available.");
                    }
                    return "grading/workspace";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Grading workspace was not found.");
                    return "redirect:/semesters";
                });
    }
}
