package com.group4.javagrader.controller;

import com.group4.javagrader.dto.SubmissionFileView;
import com.group4.javagrader.dto.SubmissionUploadGuide;
import com.group4.javagrader.dto.SubmissionUploadForm;
import com.group4.javagrader.dto.SubmissionUploadSummary;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.SubmissionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/assignments/{assignmentId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/upload")
    public String showUploadPage(
            @PathVariable("assignmentId") Long assignmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        return submissionService.findAssignment(assignmentId)
                .map(assignment -> {
                    prepareModel(model, assignment, assignmentId);
                    return "submission/upload";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Assignment not found.");
                    return "redirect:/semesters";
                });
    }

    @PostMapping("/upload")
    public String uploadArchive(
            @PathVariable("assignmentId") Long assignmentId,
            @ModelAttribute("submissionUploadForm") SubmissionUploadForm form,
            RedirectAttributes redirectAttributes) {
        form.setAssignmentId(assignmentId);

        try {
            SubmissionUploadSummary uploadSummary = submissionService.uploadArchive(assignmentId, form.getArchiveFile());
            redirectAttributes.addFlashAttribute("uploadSummary", uploadSummary);
            redirectAttributes.addFlashAttribute("uploadAnchor", "stored-submissions");
            addUploadFlashMessage(redirectAttributes, uploadSummary);
            return "redirect:/assignments/" + assignmentId + "/submissions/upload#stored-submissions";
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("uploadAnchor", "intake-upload");
            return "redirect:/assignments/" + assignmentId + "/submissions/upload#intake-upload";
        }
    }

    @GetMapping("/{submissionId}/fix-structure")
    public String showFixStructurePage(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("submissionId") Long submissionId,
            Model model,
            RedirectAttributes redirectAttributes) {
        return submissionService.findAssignment(assignmentId)
                .flatMap(assignment -> submissionService.findById(submissionId)
                        .map(submission -> {
                            if (submission.getStatus() != SubmissionStatus.REJECTED) {
                                redirectAttributes.addFlashAttribute("errorMessage",
                                        "Only rejected submissions can have their structure fixed.");
                                return "redirect:/assignments/" + assignmentId + "/submissions/upload";
                            }
                            if (!submissionService.canFixStructure(submission)) {
                                redirectAttributes.addFlashAttribute(
                                        "errorMessage",
                                        "This rejected submission cannot be fixed from the web editor because it needs source-content changes, not just path changes.");
                                return "redirect:/assignments/" + assignmentId + "/submissions/upload";
                            }

                            List<SubmissionFileView> files = submissionService.loadSubmissionFilesForViewing(submissionId);
                            Map<String, String> suggestedPaths = submissionService.buildSuggestedPaths(submissionId);
                            model.addAttribute("assignment", assignment);
                            model.addAttribute("submission", submission);
                            model.addAttribute("submissionFiles", files);
                            model.addAttribute("suggestedPaths", suggestedPaths);
                            model.addAttribute("validationCodeLabels", buildValidationCodeLabels());
                            return "submission/fix-structure";
                        }))
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Submission not found.");
                    return "redirect:/assignments/" + assignmentId + "/submissions/upload";
                });
    }

    @PostMapping("/{submissionId}/fix-structure")
    public String applyFixStructure(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("submissionId") Long submissionId,
            @RequestParam(name = "originalPath", required = false) List<String> originalPaths,
            @RequestParam(name = "newPath", required = false) List<String> newPaths,
            @RequestParam(name = "deletedPath", required = false) List<String> deletedPaths,
            RedirectAttributes redirectAttributes) {

        Map<String, String> pathMappings = new LinkedHashMap<>();
        if (originalPaths != null && newPaths != null) {
            for (int i = 0; i < Math.min(originalPaths.size(), newPaths.size()); i++) {
                String originalPath = originalPaths.get(i);
                String newPath = newPaths.get(i);
                if (originalPath != null && newPath != null && !originalPath.equals(newPath)) {
                    pathMappings.put(originalPath, newPath);
                }
            }
        }

        if (deletedPaths == null) {
            deletedPaths = new ArrayList<>();
        }

        try {
            submissionService.fixSubmissionStructure(submissionId, pathMappings, deletedPaths);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Submission structure fixed successfully! The submission is now validated and ready for grading.");
            return "redirect:/assignments/" + assignmentId + "/submissions/upload#stored-submissions";
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/assignments/" + assignmentId + "/submissions/" + submissionId + "/fix-structure";
        }
    }

    private void addUploadFlashMessage(RedirectAttributes redirectAttributes, SubmissionUploadSummary uploadSummary) {
        if (uploadSummary.getRejectedCount() == 0) {
            redirectAttributes.addFlashAttribute("successMessage", "Submission archive processed successfully.");
            return;
        }

        redirectAttributes.addFlashAttribute(
                "warningMessage",
                "Submission archive processed with "
                        + uploadSummary.getValidatedCount()
                        + " validated and "
                        + uploadSummary.getRejectedCount()
                        + " rejected.");
    }

    private void prepareModel(Model model, Assignment assignment, Long assignmentId) {
        if (!model.containsAttribute("submissionUploadForm")) {
            SubmissionUploadForm form = new SubmissionUploadForm();
            form.setAssignmentId(assignmentId);
            model.addAttribute("submissionUploadForm", form);
        }

        List<Submission> submissions = submissionService.findByAssignmentId(assignmentId);
        Map<Long, Boolean> fixableSubmissionMap = submissions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Submission::getId,
                        submissionService::canFixStructure,
                        (left, right) -> left,
                        LinkedHashMap::new));
        SubmissionUploadGuide uploadGuide = submissionService.getUploadGuide();
        long validatedCount = submissions.stream().filter(Submission::isValidated).count();
        long rejectedCount = submissions.stream().filter(Submission::isRejected).count();

        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("fixableSubmissionMap", fixableSubmissionMap);
        model.addAttribute("uploadGuide", uploadGuide);
        model.addAttribute("validationCodeLabels", buildValidationCodeLabels());
        model.addAttribute("totalSubmissions", submissions.size());
        model.addAttribute("validatedCount", validatedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("canOpenPlagiarism", validatedCount > 0);
        model.addAttribute("canRunPlagiarism", validatedCount >= 2);
    }

    private Map<String, String> buildValidationCodeLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("MAX_FILES_EXCEEDED", "Too Many Files");
        labels.put("MAX_DEPTH_EXCEEDED", "Folder Depth Exceeded");
        labels.put("EXTENSION_NOT_ALLOWED", "Unsupported File Type");
        labels.put("PACKAGE_PATH_MISMATCH", "Package Path Mismatch");
        labels.put("PACKAGE_DECLARATION_MISSING", "Package Declaration Missing");
        labels.put("JAVA_CORE_FLAT_STRUCTURE_REQUIRED", "Java Core Root Structure Required");
        labels.put("JAVA_CORE_PACKAGE_NOT_ALLOWED", "Package Declaration Not Allowed");
        labels.put("JAVA_CORE_MISSING_MAIN", "Missing Main.java");
        labels.put("OOP_JAVA_FILE_REQUIRED", "Java File Required");
        labels.put("STORAGE_ERROR", "Storage Error");
        return labels;
    }

}
