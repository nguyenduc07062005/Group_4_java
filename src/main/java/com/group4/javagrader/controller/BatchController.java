package com.group4.javagrader.controller;

import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.exception.DomainException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/assignments/{assignmentId}/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping("/precheck")
    public String showPrecheck(@PathVariable("assignmentId") Long assignmentId) {
        return "redirect:/assignments/" + assignmentId + "/grading";
    }

    @GetMapping("/{batchId}")
    public String showProgress(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("batchId") Long batchId,
            RedirectAttributes redirectAttributes) {
        return batchService.buildProgress(assignmentId, batchId)
                .map(progress -> "redirect:/assignments/" + assignmentId + "/grading#batch-progress")
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Batch progress was not found.");
                    return "redirect:/assignments/" + assignmentId + "/grading";
                });
    }

    @PostMapping("/create")
    public String createBatch(
            @PathVariable("assignmentId") Long assignmentId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            batchService.createBatch(assignmentId, resolveUsername(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Batch snapshot created.");
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/assignments/" + assignmentId + "/grading#snapshot-run";
    }

    @PostMapping("/{batchId}/start")
    public String startBatch(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("batchId") Long batchId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            batchService.startBatch(assignmentId, batchId, resolveUsername(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Batch started from the frozen snapshot.");
            return "redirect:/assignments/" + assignmentId + "/grading#batch-progress";
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/assignments/" + assignmentId + "/grading#snapshot-run";
    }

    private String resolveUsername(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
