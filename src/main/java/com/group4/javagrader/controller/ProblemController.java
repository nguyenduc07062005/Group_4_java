package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentDetailDto;
import com.group4.javagrader.dto.AssignmentSummaryDto;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterSummaryDto;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assignments/{assignmentId}/problems")
public class ProblemController {

    private final ObjectProvider<AssignmentService> assignmentServiceProvider;
    private final ObjectProvider<ProblemService> problemServiceProvider;
    private final ObjectProvider<SemesterService> semesterServiceProvider;

    public ProblemController(
            ObjectProvider<AssignmentService> assignmentServiceProvider,
            ObjectProvider<ProblemService> problemServiceProvider,
            ObjectProvider<SemesterService> semesterServiceProvider) {
        this.assignmentServiceProvider = assignmentServiceProvider;
        this.problemServiceProvider = problemServiceProvider;
        this.semesterServiceProvider = semesterServiceProvider;
    }

    @GetMapping("/create")
    public String showCreateForm(@PathVariable Long assignmentId, Model model) {
        AssignmentService assignmentService = assignmentServiceProvider.getIfAvailable();
        AssignmentDetailDto assignment = assignmentService != null ? assignmentService.findDetailById(assignmentId).orElse(null) : null;
        if (assignment == null) {
            model.addAttribute("errorMessage", "Assignment not found.");
            model.addAttribute("semesters", getSemesters());
            model.addAttribute("assignments", getAssignments());
            return "dashboard/index";
        }

        ProblemForm problemForm = new ProblemForm();
        problemForm.setProblemOrder(assignment.getProblemCount() + 1);
        model.addAttribute("problemForm", problemForm);
        model.addAttribute("assignment", assignment);
        return "problem/create";
    }

    @PostMapping("/create")
    public String create(
            @PathVariable Long assignmentId,
            @Valid @ModelAttribute("problemForm") ProblemForm problemForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        AssignmentService assignmentService = assignmentServiceProvider.getIfAvailable();
        ProblemService problemService = problemServiceProvider.getIfAvailable();
        AssignmentDetailDto assignment = assignmentService != null ? assignmentService.findDetailById(assignmentId).orElse(null) : null;
        if (assignment == null) {
            model.addAttribute("errorMessage", "Assignment not found.");
            model.addAttribute("semesters", getSemesters());
            model.addAttribute("assignments", getAssignments());
            return "dashboard/index";
        }

        if (problemService == null) {
            bindingResult.reject("serviceUnavailable", "ProblemService is not available yet.");
        } else if (problemService.existsByAssignmentIdAndProblemOrder(assignmentId, problemForm.getProblemOrder())) {
            bindingResult.rejectValue("problemOrder", "duplicate", "Problem order already exists in this assignment.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("assignment", assignment);
            return "problem/create";
        }

        problemService.create(assignmentId, problemForm);
        redirectAttributes.addFlashAttribute("successMessage", "Problem added successfully.");
        return "redirect:/assignments/" + assignmentId;
    }

    private List<SemesterSummaryDto> getSemesters() {
        SemesterService service = semesterServiceProvider.getIfAvailable();
        return service != null ? service.findAllSummaries() : List.of();
    }

    private List<AssignmentSummaryDto> getAssignments() {
        AssignmentService service = assignmentServiceProvider.getIfAvailable();
        return service != null ? service.findAllSummaries() : List.of();
    }
}
