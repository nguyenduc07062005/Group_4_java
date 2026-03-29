package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("assignmentForm")) {
            model.addAttribute("assignmentForm", new AssignmentForm());
        }
        return "assignment/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "assignment/create";
        }

        try {
            Long assignmentId = assignmentService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment configuration saved successfully.");
            return "redirect:/assignments/" + assignmentId;
        } catch (AssignmentConfigValidationException ex) {
            bindingResult.rejectValue(ex.getFieldName(), ex.getFieldName() + ".invalid", ex.getMessage());
            return "assignment/create";
        }
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return assignmentService.findById(id)
                .map(assignment -> {
                    model.addAttribute("assignment", assignment);
                    return "assignment/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
                    return "redirect:/assignments/create";
                });
    }
}
