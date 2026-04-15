package com.group4.javagrader.controller;

import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
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

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final SemesterService semesterService;

    public CourseController(CourseService courseService, SemesterService semesterService) {
        this.courseService = courseService;
        this.semesterService = semesterService;
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam(value = "semesterId", required = false) Long semesterId, Model model) {
        if (!model.containsAttribute("courseForm")) {
            CourseForm form = new CourseForm();
            form.setSemesterId(semesterId);
            model.addAttribute("courseForm", form);
        }
        populateFormPage(model, false, null);
        return "course/create";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return courseService.findById(id)
                .map(course -> {
                    if (!model.containsAttribute("courseForm")) {
                        CourseForm form = new CourseForm();
                        form.setSemesterId(course.getSemester().getId());
                        form.setCourseCode(course.getCourseCode());
                        form.setCourseName(course.getCourseName());
                        form.setWeekCount(course.getWeekCount());
                        model.addAttribute("courseForm", form);
                    }
                    populateFormPage(model, true, id);
                    return "course/create";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Course not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("courseForm") CourseForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormPage(model, false, null);
            return "course/create";
        }

        try {
            Long courseId = courseService.create(form);
            Long semesterId = courseService.findById(courseId)
                    .map(course -> course.getSemester().getId())
                    .orElse(form.getSemesterId());
            redirectAttributes.addFlashAttribute("successMessage", "Course created successfully.");
            return "redirect:/semesters/" + semesterId;
        } catch (AssignmentConfigValidationException ex) {
            bindingResult.rejectValue(ex.getFieldName(), ex.getFieldName() + ".invalid", ex.getMessage());
            populateFormPage(model, false, null);
            return "course/create";
        } catch (DomainException ex) {
            bindingResult.reject("courseForm.invalid", ex.getMessage());
            populateFormPage(model, false, null);
            return "course/create";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("courseForm") CourseForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormPage(model, true, id);
            return "course/create";
        }

        try {
            courseService.update(id, form);
            Long semesterId = courseService.findById(id)
                    .map(course -> course.getSemester().getId())
                    .orElse(form.getSemesterId());
            redirectAttributes.addFlashAttribute("successMessage", "Course updated successfully.");
            return "redirect:/semesters/" + semesterId;
        } catch (AssignmentConfigValidationException ex) {
            bindingResult.rejectValue(ex.getFieldName(), ex.getFieldName() + ".invalid", ex.getMessage());
            populateFormPage(model, true, id);
            return "course/create";
        } catch (DomainException ex) {
            bindingResult.reject("courseForm.invalid", ex.getMessage());
            populateFormPage(model, true, id);
            return "course/create";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Long semesterId = courseService.findById(id)
                .map(course -> course.getSemester().getId())
                .orElse(null);
        try {
            courseService.archive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted successfully.");
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return semesterId != null ? "redirect:/semesters/" + semesterId : "redirect:/semesters";
    }

    private void populateFormPage(Model model, boolean editMode, Long courseId) {
        model.addAttribute("pageTitle", editMode ? "Edit Course" : "Create Course");
        model.addAttribute("pageSubtitle", editMode ? "Update this course." : "Create a course inside a semester, then let teachers grade by week or custom assignment.");
        model.addAttribute("submitLabel", editMode ? "Update Course" : "Save Course");
        model.addAttribute("editMode", editMode);
        model.addAttribute("courseId", courseId);
        model.addAttribute("semesters", semesterService.findActiveSemesters());
    }
}
