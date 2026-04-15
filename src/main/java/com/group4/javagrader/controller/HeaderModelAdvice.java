package com.group4.javagrader.controller;

import com.group4.javagrader.dto.HeaderNotificationItem;
import com.group4.javagrader.dto.HeaderSemesterSummary;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.security.CustomUserDetails;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = Controller.class)
public class HeaderModelAdvice {

    private final SemesterService semesterService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final HeaderPageMetadataResolver pageMetadataResolver;

    public HeaderModelAdvice(
            SemesterService semesterService,
            CourseService courseService,
            AssignmentService assignmentService,
            HeaderPageMetadataResolver pageMetadataResolver) {
        this.semesterService = semesterService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.pageMetadataResolver = pageMetadataResolver;
    }

    @ModelAttribute
    public void populateHeaderModel(Model model, Authentication authentication, HttpServletRequest request) {
        String requestUri = request != null ? request.getRequestURI() : "";
        if (!isInteractiveHtmlRequest(request)) {
            return;
        }

        HeaderPageMetadataResolver.HeaderPageMetadata pageMetadata = pageMetadataResolver.resolve(requestUri);
        model.addAttribute("pageDescription", pageMetadata.pageDescription());
        model.addAttribute("pageRobots", pageMetadata.pageRobots());

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            model.addAttribute("headerAuthenticated", false);
            return;
        }

        List<Semester> semesters = semesterService.findActiveSemesters();
        List<HeaderSemesterSummary> semesterSummaries = new ArrayList<>();
        int totalCourseCount = 0;
        int totalAssignmentCount = 0;

        if (!semesters.isEmpty()) {
            List<Long> semesterIds = semesters.stream()
                    .map(Semester::getId)
                    .toList();
            Map<Long, Integer> courseCountsBySemester = countCoursesBySemester(semesterIds);
            Map<Long, Integer> assignmentCountsBySemester = countAssignmentsBySemester(semesterIds);
            totalCourseCount = courseCountsBySemester.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            totalAssignmentCount = assignmentCountsBySemester.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            for (Semester semester : semesters) {
                semesterSummaries.add(new HeaderSemesterSummary(
                        semester.getId(),
                        semester.getCode(),
                        semester.getName(),
                        courseCountsBySemester.getOrDefault(semester.getId(), 0),
                        assignmentCountsBySemester.getOrDefault(semester.getId(), 0)));
            }
        }

        List<HeaderNotificationItem> notifications = buildNotifications(
                semesters,
                totalCourseCount,
                totalAssignmentCount);

        String fullName = "Teacher";
        String username = authentication.getName();
        if (authentication.getPrincipal() instanceof CustomUserDetails details) {
            fullName = details.getFullName();
            username = details.getUsername();
        }

        String roleStr = authentication.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.joining(", "));

        model.addAttribute("headerAuthenticated", true);
        model.addAttribute("headerUserFullName", fullName);
        model.addAttribute("headerUserRole", roleStr);
        model.addAttribute("headerUserInitials", initialsOf(fullName));
        model.addAttribute("headerSemesterCount", semesters.size());
        model.addAttribute("headerCourseCount", totalCourseCount);
        model.addAttribute("headerAssignmentCount", totalAssignmentCount);
        model.addAttribute("headerSemesterSummaries", semesterSummaries.stream().limit(4).toList());
        model.addAttribute("headerNotificationItems", notifications);
        model.addAttribute("headerNotificationCount", notifications.size());
        model.addAttribute("headerPageTitle", pageMetadata.pageTitle());
        model.addAttribute("headerPageTrail", pageMetadata.pageTrail());
        model.addAttribute("headerPrimarySection", pageMetadata.primarySection());
    }

    private Map<Long, Integer> countCoursesBySemester(List<Long> semesterIds) {
        return courseService.findBySemesterIds(semesterIds).stream()
                .collect(Collectors.groupingBy(
                        course -> course.getSemester().getId(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
    }

    private Map<Long, Integer> countAssignmentsBySemester(List<Long> semesterIds) {
        return assignmentService.findBySemesterIds(semesterIds).stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getSemester().getId(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
    }

    private List<HeaderNotificationItem> buildNotifications(
            List<Semester> semesters,
            int totalCourseCount,
            int totalAssignmentCount) {
        List<HeaderNotificationItem> notifications = new ArrayList<>();

        if (semesters.isEmpty()) {
            notifications.add(new HeaderNotificationItem(
                    "No semester available",
                    "Create the first semester to start organizing courses and weekly grading.",
                    "warning",
                    "/semesters/create"));
            return notifications;
        }

        Semester latestSemester = semesters.get(0);
        notifications.add(new HeaderNotificationItem(
                "Semester board is active",
                semesters.size() + " semester(s) are available. Latest semester: "
                        + latestSemester.getCode() + " - " + latestSemester.getName() + ".",
                "info",
                "/semesters"));

        notifications.add(new HeaderNotificationItem(
                totalCourseCount == 0 ? "No course configured yet" : "Course flow is ready",
                totalCourseCount == 0
                        ? "Create a course inside a semester before opening weekly assignments."
                        : totalCourseCount + " course(s) are available for weekly grading structure.",
                totalCourseCount == 0 ? "warning" : "success",
                "/semesters/" + latestSemester.getId()));

        notifications.add(new HeaderNotificationItem(
                totalAssignmentCount == 0 ? "No assignment found" : "Assignments available",
                totalAssignmentCount == 0
                        ? "Create an assignment after choosing a semester and course."
                        : totalAssignmentCount + " assignment workspace(s) are currently available across the active semesters.",
                totalAssignmentCount == 0 ? "warning" : "info",
                "/semesters"));

        return notifications;
    }

    private String initialsOf(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isEmpty()) {
            return "T";
        }

        String[] parts = normalized.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }

        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private boolean isInteractiveHtmlRequest(HttpServletRequest request) {
        if (request == null) {
            return true;
        }

        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return false;
        }

        String accept = request.getHeader("Accept");
        if (accept == null || accept.isBlank()) {
            return true;
        }

        String normalizedAccept = accept.toLowerCase(Locale.ROOT);
        if (normalizedAccept.contains("text/html") || normalizedAccept.contains("application/xhtml+xml")) {
            return true;
        }
        return false;
    }
}
