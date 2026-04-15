package com.group4.javagrader.controller;

import com.group4.javagrader.service.ResultService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assignments/{assignmentId}/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping
    public String showIndex(@PathVariable("assignmentId") Long assignmentId) {
        return "redirect:/assignments/" + assignmentId + "/grading#results-overview";
    }

    @GetMapping("/{resultId}")
    public String showDetail(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("resultId") Long resultId,
            RedirectAttributes redirectAttributes) {
        return resultService.buildDetail(assignmentId, resultId)
                .map(view -> "redirect:/assignments/" + assignmentId + "/grading?resultId=" + resultId + "#result-detail")
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Result detail was not found.");
                    return "redirect:/assignments/" + assignmentId + "/grading#results-overview";
                });
    }
}
