package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentDetailDto;
import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.AssignmentSummaryDto;
import com.group4.javagrader.dto.SemesterSummaryDto;
import com.group4.javagrader.service.AssignmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assignments")
public class AssignmentController {

    private final ObjectProvider<AssignmentService> assignmentServiceProvider;
    private final ObjectProvider<SemesterService> semesterServiceProvider;

    public AssignmentController(
            ObjectProvider<AssignmentService> assignmentServiceProvider,
            ObjectProvider<SemesterService> semesterServiceProvider) {
        this.assignmentServiceProvider = assignmentServiceProvider;
        this.semesterServiceProvider = semesterServiceProvider;
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) Long semesterId, Model model) {
        SemesterService semesterService = semesterServiceProvider.getIfAvailable();
        AssignmentForm assignmentForm = new AssignmentForm();
        if (semesterService != null && semesterId != null && semesterService.existsById(semesterId)) {
            assignmentForm.setSemesterId(semesterId);
        }
        model.addAttribute("assignmentForm", assignmentForm);
        populateCreateModel(model);
        return "assignment/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("assignmentForm") AssignmentForm assignmentForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        SemesterService semesterService = semesterServiceProvider.getIfAvailable();
        AssignmentService assignmentService = assignmentServiceProvider.getIfAvailable();

        if (semesterService == null || assignmentService == null) {
            bindingResult.reject("serviceUnavailable", "Assignment services are not available yet.");
        } else if (!semesterService.existsById(assignmentForm.getSemesterId())) {
            bindingResult.rejectValue("semesterId", "notFound", "Please choose an existing semester.");
        }

        if (bindingResult.hasErrors()) {
            populateCreateModel(model);
            return "assignment/create";
        }

        Long assignmentId = assignmentService.create(assignmentForm);
        redirectAttributes.addFlashAttribute("successMessage", "Assignment created successfully.");
        return "redirect:/assignments/" + assignmentId;
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long id, Model model) {
        AssignmentService assignmentService = assignmentServiceProvider.getIfAvailable();
        AssignmentDetailDto assignment = assignmentService != null ? assignmentService.findDetailById(id).orElse(null) : null;
        if (assignment == null) {
            model.addAttribute("errorMessage", "Assignment not found.");
            model.addAttribute("semesters", getSemesters());
            model.addAttribute("assignments", getAssignments());
            return "dashboard/index";
        }

        model.addAttribute("assignment", assignment);
        model.addAttribute("problems", assignment.getProblems());
        model.addAttribute(
                "testCases",
                assignment.getProblems().stream().flatMap(problem -> problem.getTestCases().stream()).toList());
        return "assignment/detail";
    }

    private void populateCreateModel(Model model) {
        model.addAttribute("semesters", getSemesters());
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
