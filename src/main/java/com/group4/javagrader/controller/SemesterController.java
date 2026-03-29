package com.group4.javagrader.controller;

import com.group4.javagrader.dto.SemesterDetailDto;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.SemesterSummaryDto;
import com.group4.javagrader.dto.AssignmentSummaryDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/semesters")
public class SemesterController {

    private final ObjectProvider<SemesterService> semesterServiceProvider;
    private final ObjectProvider<AssignmentService> assignmentServiceProvider;

    public SemesterController(
            ObjectProvider<SemesterService> semesterServiceProvider,
            ObjectProvider<AssignmentService> assignmentServiceProvider) {
        this.semesterServiceProvider = semesterServiceProvider;
        this.assignmentServiceProvider = assignmentServiceProvider;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("semesterForm", new SemesterForm());
        return "semester/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("semesterForm") SemesterForm semesterForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        SemesterService semesterService = semesterServiceProvider.getIfAvailable();

        if (semesterForm.getStartDate() != null
                && semesterForm.getEndDate() != null
                && semesterForm.getEndDate().isBefore(semesterForm.getStartDate())) {
            bindingResult.rejectValue("endDate", "dateOrder", "End date must be after or equal to the start date.");
        }

        if (semesterService == null) {
            bindingResult.reject("serviceUnavailable", "SemesterService is not available yet.");
        } else if (semesterService.existsByCode(semesterForm.getCode())) {
            bindingResult.rejectValue("code", "duplicate", "Semester code already exists.");
        }

        if (bindingResult.hasErrors()) {
            return "semester/create";
        }

        semesterService.create(semesterForm);
        redirectAttributes.addFlashAttribute("successMessage", "Semester created successfully.");
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long id, Model model) {
        SemesterService semesterService = semesterServiceProvider.getIfAvailable();
        SemesterDetailDto semester = semesterService != null ? semesterService.findDetailById(id).orElse(null) : null;
        if (semester == null) {
            model.addAttribute("errorMessage", "Semester not found.");
            model.addAttribute("semesters", getSemesters());
            model.addAttribute("assignments", getAssignments());
            return "dashboard/index";
        }

        model.addAttribute("semester", semester);
        model.addAttribute("assignments", semester.getAssignments());
        return "semester/detail";
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
