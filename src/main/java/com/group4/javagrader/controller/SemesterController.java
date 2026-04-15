package com.group4.javagrader.controller;

import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.SemesterBoardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.TeacherBoardService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/semesters")
public class SemesterController {

    private final SemesterService semesterService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final TeacherBoardService teacherBoardService;

    public SemesterController(
            SemesterService semesterService,
            CourseService courseService,
            AssignmentService assignmentService,
            TeacherBoardService teacherBoardService) {
        this.semesterService = semesterService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.teacherBoardService = teacherBoardService;
    }

    @GetMapping
    public String showOverview(Model model) {
        List<SemesterBoardView> semesterBoards = teacherBoardService.buildActiveSemesterBoard();
        List<Semester> semesters = semesterBoards.stream().map(SemesterBoardView::getSemester).toList();
        Map<Long, Integer> assignmentCountsBySemesterId = new LinkedHashMap<>();
        Map<Long, Boolean> assignmentCanDelete = new LinkedHashMap<>();

        for (SemesterBoardView semesterBoard : semesterBoards) {
            Semester semester = semesterBoard.getSemester();
            List<Assignment> assignments = assignmentService.findBySemesterId(semester.getId());
            assignmentCountsBySemesterId.put(semester.getId(), assignments.size());
            for (Assignment assignment : assignments) {
                assignmentCanDelete.put(assignment.getId(), assignmentService.canDelete(assignment.getId()));
            }
        }

        model.addAttribute("semesters", semesters);
        model.addAttribute("semesterBoards", semesterBoards);
        model.addAttribute("assignmentCountsBySemesterId", assignmentCountsBySemesterId);
        model.addAttribute("assignmentCanDelete", assignmentCanDelete);
        return "semester/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("semesterForm")) {
            model.addAttribute("semesterForm", new SemesterForm());
        }
        populateFormPage(model, false, null);
        return "semester/create";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        return semesterService.findActiveById(id)
                .map(semester -> {
                    if (!model.containsAttribute("semesterForm")) {
                        model.addAttribute("semesterForm", toForm(semester));
                    }
                    populateFormPage(model, true, id);
                    return "semester/create";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Semester not found.");
                    return "redirect:/semesters";
                });
    }

    @GetMapping("/{id}")
    public String showDetail(
            @PathVariable("id") Long id,
            @RequestParam(value = "highlightCourseId", required = false) Long highlightCourseId,
            Model model,
            RedirectAttributes redirectAttributes) {
        return semesterService.findActiveById(id)
                .map(semester -> {
                    SemesterBoardView semesterBoard = teacherBoardService.buildSemesterBoard(id).orElse(null);
                    List<Course> courses = courseService.findBySemesterId(id);
                    List<Assignment> assignments = assignmentService.findBySemesterId(id);
                    Long effectiveHighlightCourseId = resolveHighlightCourseId(courses, highlightCourseId);
                    Map<Long, Boolean> assignmentCanDelete = new LinkedHashMap<>();
                    for (Assignment assignment : assignments) {
                        assignmentCanDelete.put(assignment.getId(), assignmentService.canDelete(assignment.getId()));
                    }
                    model.addAttribute("semester", semester);
                    model.addAttribute("courses", courses);
                    model.addAttribute("courseCount", courses.size());
                    model.addAttribute("latestCourse", courses.isEmpty() ? null : courses.get(0));
                    model.addAttribute("assignments", assignments);
                    model.addAttribute("assignmentCount", assignments.size());
                    model.addAttribute("latestAssignment", assignments.isEmpty() ? null : assignments.get(0));
                    model.addAttribute("assignmentCanDelete", assignmentCanDelete);
                    model.addAttribute("highlightCourseId", effectiveHighlightCourseId);
                    model.addAttribute("semesterBoard", semesterBoard);
                    return "semester/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Semester not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("semesterForm") SemesterForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormPage(model, false, null);
            return "semester/create";
        }

        try {
            Long semesterId = semesterService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Semester created successfully.");
            return "redirect:/semesters/" + semesterId;
        } catch (DomainException ex) {
            bindingResult.reject("semesterForm.invalid", ex.getMessage());
            populateFormPage(model, false, null);
            return "semester/create";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("semesterForm") SemesterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            populateFormPage(model, true, id);
            return "semester/create";
        }

        try {
            semesterService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Semester updated successfully.");
            return "redirect:/semesters/" + id;
        } catch (DomainException ex) {
            bindingResult.reject("semesterForm.invalid", ex.getMessage());
            populateFormPage(model, true, id);
            return "semester/create";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            semesterService.archive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Semester deleted successfully.");
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/semesters";
    }

    private void populateFormPage(Model model, boolean editMode, Long semesterId) {
        model.addAttribute("pageTitle", editMode ? "Edit Semester" : "Create Semester");
        model.addAttribute("pageSubtitle", editMode ? "Update this semester workspace." : "Create a new semester workspace.");
        model.addAttribute("submitLabel", editMode ? "Update Semester" : "Save Semester");
        model.addAttribute("editMode", editMode);
        model.addAttribute("semesterId", semesterId);
    }

    private SemesterForm toForm(Semester semester) {
        SemesterForm form = new SemesterForm();
        form.setCode(semester.getCode());
        form.setName(semester.getName());
        form.setStartDate(semester.getStartDate());
        form.setEndDate(semester.getEndDate());
        return form;
    }

    private Long resolveHighlightCourseId(List<Course> courses, Long highlightCourseId) {
        if (courses.isEmpty()) {
            return null;
        }
        if (highlightCourseId == null) {
            return courses.get(0).getId();
        }
        return courses.stream()
                .map(Course::getId)
                .filter(highlightCourseId::equals)
                .findFirst()
                .orElse(courses.get(0).getId());
    }
}
