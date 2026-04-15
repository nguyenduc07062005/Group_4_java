package com.group4.javagrader.controller;

import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.PlagiarismService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/assignments/{assignmentId}/plagiarism")
public class PlagiarismController {

    private final PlagiarismService plagiarismService;

    public PlagiarismController(PlagiarismService plagiarismService) {
        this.plagiarismService = plagiarismService;
    }

    @GetMapping
    public String showDashboard(@PathVariable("assignmentId") Long assignmentId, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("dashboard", plagiarismService.buildDashboard(assignmentId));
            return "plagiarism/index";
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/semesters";
        }
    }

    @PostMapping("/run")
    public String runReport(
            @PathVariable("assignmentId") Long assignmentId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            plagiarismService.runReport(assignmentId, resolveUsername(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Plagiarism check completed successfully.");
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/assignments/" + assignmentId + "/plagiarism";
    }

    @PostMapping("/pairs/{pairId}/override")
    public String overridePair(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("pairId") Long pairId,
            @RequestParam("decision") String decision,
            @RequestParam(value = "note", required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            plagiarismService.overridePair(assignmentId, pairId, decision, note, resolveUsername(principal));
            redirectAttributes.addFlashAttribute("successMessage", "Plagiarism override saved.");
        } catch (DomainException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/assignments/" + assignmentId + "/plagiarism";
    }

    private String resolveUsername(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
