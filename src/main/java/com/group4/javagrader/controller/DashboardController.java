package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentSummaryDto;
import com.group4.javagrader.dto.SemesterSummaryDto;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.SemesterService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ObjectProvider<SemesterService> semesterServiceProvider;
    private final ObjectProvider<AssignmentService> assignmentServiceProvider;

    public DashboardController(
            ObjectProvider<SemesterService> semesterServiceProvider,
            ObjectProvider<AssignmentService> assignmentServiceProvider) {
        this.semesterServiceProvider = semesterServiceProvider;
        this.assignmentServiceProvider = assignmentServiceProvider;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("semesters", getSemesters());
        model.addAttribute("assignments", getAssignments());
        return "dashboard/index";
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
