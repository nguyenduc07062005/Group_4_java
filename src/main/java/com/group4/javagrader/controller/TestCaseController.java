package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentDetailDto;
import com.group4.javagrader.dto.AssignmentSummaryDto;
import com.group4.javagrader.dto.ProblemDetailDto;
import com.group4.javagrader.dto.SemesterSummaryDto;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.TestCaseService;
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
@RequestMapping("/problems/{problemId}/testcases")
public class TestCaseController {

    private final ObjectProvider<ProblemService> problemServiceProvider;
    private final ObjectProvider<AssignmentService> assignmentServiceProvider;
    private final ObjectProvider<TestCaseService> testCaseServiceProvider;
    private final ObjectProvider<SemesterService> semesterServiceProvider;

    public TestCaseController(
            ObjectProvider<ProblemService> problemServiceProvider,
            ObjectProvider<AssignmentService> assignmentServiceProvider,
            ObjectProvider<TestCaseService> testCaseServiceProvider,
            ObjectProvider<SemesterService> semesterServiceProvider) {
        this.problemServiceProvider = problemServiceProvider;
        this.assignmentServiceProvider = assignmentServiceProvider;
        this.testCaseServiceProvider = testCaseServiceProvider;
        this.semesterServiceProvider = semesterServiceProvider;
    }

    @GetMapping("/create")
    public String showCreateForm(@PathVariable Long problemId, Model model) {
        ProblemService problemService = problemServiceProvider.getIfAvailable();
        AssignmentService assignmentService = assignmentServiceProvider.getIfAvailable();
        ProblemDetailDto problem = problemService != null ? problemService.findDetailById(problemId).orElse(null) : null;
        if (problem == null) {
            model.addAttribute("errorMessage", "Problem not found.");
            model.addAttribute("semesters", getSemesters());
            model.addAttribute("assignments", getAssignments());
            return "dashboard/index";
        }

        AssignmentDetailDto assignment = assignmentService != null ? assignmentService.findDetailById(problem.getAssignmentId()).orElse(null) : null;
        TestCaseForm testCaseForm = new TestCaseForm();
        testCaseForm.setCaseOrder(problem.getTestCaseCount() + 1);
        model.addAttribute("testCaseForm", testCaseForm);
        model.addAttribute("problem", problem);
        model.addAttribute("assignment", assignment);
        return "testcase/create";
    }

    @PostMapping("/create")
    public String create(
            @PathVariable Long problemId,
            @Valid @ModelAttribute("testCaseForm") TestCaseForm testCaseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        ProblemService problemService = problemServiceProvider.getIfAvailable();
        AssignmentService assignmentService = assignmentServiceProvider.getIfAvailable();
        TestCaseService testCaseService = testCaseServiceProvider.getIfAvailable();
        ProblemDetailDto problem = problemService != null ? problemService.findDetailById(problemId).orElse(null) : null;
        if (problem == null) {
            model.addAttribute("errorMessage", "Problem not found.");
            model.addAttribute("semesters", getSemesters());
            model.addAttribute("assignments", getAssignments());
            return "dashboard/index";
        }

        AssignmentDetailDto assignment = assignmentService != null ? assignmentService.findDetailById(problem.getAssignmentId()).orElse(null) : null;
        if (testCaseService == null) {
            bindingResult.reject("serviceUnavailable", "TestCaseService is not available yet.");
        } else if (testCaseService.existsByProblemIdAndCaseOrder(problemId, testCaseForm.getCaseOrder())) {
            bindingResult.rejectValue("caseOrder", "duplicate", "Case order already exists in this problem.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("problem", problem);
            model.addAttribute("assignment", assignment);
            return "testcase/create";
        }

        testCaseService.create(problemId, testCaseForm);
        redirectAttributes.addFlashAttribute("successMessage", "Test case added successfully.");
        return "redirect:/assignments/" + problem.getAssignmentId();
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
