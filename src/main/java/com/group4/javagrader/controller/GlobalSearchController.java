package com.group4.javagrader.controller;

import com.group4.javagrader.service.GlobalSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    public GlobalSearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(name = "q", required = false) String query,
            RedirectAttributes redirectAttributes) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            redirectAttributes.addFlashAttribute("warningMessage", "Enter a keyword to search semesters, courses, or assignments.");
            return "redirect:/dashboard";
        }

        String destination = globalSearchService.resolveDestination(normalizedQuery);
        if ("redirect:/semesters".equals(destination)) {
            redirectAttributes.addFlashAttribute("warningMessage", "No workspace matched \"" + normalizedQuery + "\".");
        }
        return destination;
    }
}
