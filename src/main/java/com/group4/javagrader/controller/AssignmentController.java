package com.group4.javagrader.controller;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SemesterService semesterService;
    private final CourseService courseService;
    private final ProblemService problemService;

    public AssignmentController(
            AssignmentService assignmentService,
            SemesterService semesterService,
            CourseService courseService,
            ProblemService problemService) {
        this.assignmentService = assignmentService;
        this.semesterService = semesterService;
        this.courseService = courseService;
        this.problemService = problemService;
    }

    @GetMapping("/create")
    public String showCreateForm(
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            Model model) {
        if (!model.containsAttribute("assignmentForm")) {
            AssignmentForm form = new AssignmentForm();
            form.setSemesterId(semesterId);
            form.setCourseId(courseId);
            model.addAttribute("assignmentForm", form);
        }
        populateFormModel(model, false, null);
        return "assignment/create";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return assignmentService.findById(id)
                .map(assignment -> {
                    if (!model.containsAttribute("assignmentForm")) {
                        model.addAttribute("assignmentForm", toForm(assignment));
                    }
                    populateFormModel(model, true, assignment);
                    return "assignment/create";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateFormModel(model, false, null);
            return "assignment/create";
        }

        try {
            Long assignmentId = assignmentService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment configuration saved successfully.");
            return "redirect:/assignments/" + assignmentId;
        } catch (AssignmentConfigValidationException ex) {
            bindingResult.rejectValue(ex.getFieldName(), ex.getFieldName() + ".invalid", ex.getMessage());
            populateFormModel(model, false, null);
            return "assignment/create";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("assignmentForm") AssignmentForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Assignment assignment = assignmentService.findById(id).orElse(null);
        if (assignment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
            return "redirect:/semesters";
        }

        if (bindingResult.hasErrors()) {
            populateFormModel(model, true, assignment);
            return "assignment/create";
        }

        try {
            assignmentService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment updated successfully.");
            return "redirect:/assignments/" + id;
        } catch (AssignmentConfigValidationException ex) {
            bindingResult.rejectValue(ex.getFieldName(), ex.getFieldName() + ".invalid", ex.getMessage());
            populateFormModel(model, true, assignment);
            return "assignment/create";
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/semesters";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        return assignmentService.findById(id)
                .map(assignment -> {
                    Long semesterId = assignment.getSemester().getId();
                    try {
                        assignmentService.delete(id);
                        redirectAttributes.addFlashAttribute("successMessage", "Assignment deleted successfully.");
                        return "redirect:/semesters/" + semesterId;
                    } catch (DomainException ex) {
                        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                        return "redirect:/assignments/" + id;
                    }
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
                    return "redirect:/semesters";
                });
    }

    @GetMapping("/{id}/description-file")
    public ResponseEntity<byte[]> downloadDescriptionFile(@PathVariable("id") Long id) {
        return assignmentService.findById(id)
                .filter(Assignment::hasDescriptionUpload)
                .flatMap(assignment -> assignmentService.findAttachmentData(id, AssignmentAttachmentType.DESCRIPTION)
                        .map(data -> buildDownloadResponse(
                                assignment,
                                assignment.getDescriptionFileName(),
                                assignment.getDescriptionFileContentType(),
                                data)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/oop-rule-config")
    public ResponseEntity<byte[]> downloadOopRuleConfig(@PathVariable("id") Long id) {
        return assignmentService.findById(id)
                .filter(Assignment::hasOopRuleConfigUpload)
                .flatMap(assignment -> assignmentService.findAttachmentData(id, AssignmentAttachmentType.OOP_RULE_CONFIG)
                        .map(data -> buildDownloadResponse(
                                assignment,
                                assignment.getOopRuleConfigFileName(),
                                assignment.getOopRuleConfigContentType(),
                                data)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> buildDownloadResponse(
            Assignment assignment,
            String fileName,
            String contentType,
            byte[] data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentType(resolveMediaType(contentType));
        headers.setContentLength(data.length);
        headers.add("X-Assignment-Id", String.valueOf(assignment.getId()));
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    private MediaType resolveMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private void populateFormModel(Model model, boolean editMode, Assignment assignment) {
        List<com.group4.javagrader.entity.Semester> semesters = semesterService.findActiveSemesters();
        Map<Long, List<com.group4.javagrader.entity.Course>> coursesBySemesterId = new LinkedHashMap<>();
        for (com.group4.javagrader.entity.Semester semester : semesters) {
            coursesBySemesterId.put(semester.getId(), courseService.findBySemesterId(semester.getId()));
        }
        model.addAttribute("semesters", semesters);
        model.addAttribute("coursesBySemesterId", coursesBySemesterId);
        model.addAttribute("pageTitle", editMode ? "Edit Assignment" : "Create Assignment");
        model.addAttribute("pageSubtitle", null);
        model.addAttribute("submitLabel", editMode ? "Update Assignment" : "Save Assignment");
        model.addAttribute("editMode", editMode);
        model.addAttribute("currentAssignment", assignment);
    }

    private AssignmentForm toForm(Assignment assignment) {
        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName(assignment.getAssignmentName());
        form.setSemesterId(assignment.getSemester().getId());
        form.setCourseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null);
        form.setGradingMode(assignment.getGradingMode());
        form.setAssignmentType(assignment.getAssignmentType());
        form.setWeekNumber(assignment.getWeekNumber());
        form.setPlagiarismThreshold(assignment.getPlagiarismThreshold().intValue());
        form.setOutputNormalizationPolicy(assignment.getOutputNormalizationPolicy());
        form.setInputMode(problemService.findAssignmentSettingsProblemByAssignmentId(assignment.getId())
                .map(Problem::getInputMode)
                .orElse(InputMode.STDIN));
        form.setLogicWeight(assignment.getLogicWeight());
        form.setOopWeight(assignment.getOopWeight());
        return form;
    }

}
