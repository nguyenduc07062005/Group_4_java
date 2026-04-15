package com.group4.javagrader.controller;

import org.springframework.stereotype.Component;

@Component
public class HeaderPageMetadataResolver {

    public HeaderPageMetadata resolve(String requestUri) {
        return new HeaderPageMetadata(
                resolvePageDescription(requestUri),
                resolvePageRobots(requestUri),
                resolvePageTitle(requestUri),
                resolvePageTrail(requestUri),
                resolvePrimarySection(requestUri));
    }

    public record HeaderPageMetadata(
            String pageDescription,
            String pageRobots,
            String pageTitle,
            String pageTrail,
            String primarySection) {
    }

    private String resolvePageTitle(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "Dashboard";
        }
        if (requestUri.startsWith("/dashboard")) {
            return "Dashboard";
        }
        if (requestUri.startsWith("/semesters/create")) {
            return "Create Semester";
        }
        if (requestUri.matches("^/semesters/\\d+/edit$")) {
            return "Edit Semester";
        }
        if (requestUri.matches("^/semesters/\\d+$")) {
            return "Semester";
        }
        if (requestUri.startsWith("/semesters")) {
            return "Semesters";
        }
        if (requestUri.startsWith("/courses/create")) {
            return "Create Course";
        }
        if (requestUri.startsWith("/courses")) {
            return "Course";
        }
        if (requestUri.startsWith("/assignments/create")) {
            return "Create Assignment";
        }
        if (requestUri.matches("^/assignments/\\d+/grading$")) {
            return "Grading Workspace";
        }
        if (requestUri.matches("^/assignments/\\d+/submissions/upload$")) {
            return "Upload Submissions";
        }
        if (requestUri.matches("^/assignments/\\d+/plagiarism.*")) {
            return "Plagiarism Review";
        }
        if (requestUri.matches("^/assignments/\\d+/batches/precheck$")) {
            return "Batch Precheck";
        }
        if (requestUri.matches("^/assignments/\\d+/batches/\\d+$")) {
            return "Batch Progress";
        }
        if (requestUri.matches("^/assignments/\\d+/results/\\d+$")) {
            return "Result Detail";
        }
        if (requestUri.matches("^/assignments/\\d+/results$")) {
            return "Results";
        }
        if (requestUri.matches("^/assignments/\\d+/reports$")) {
            return "Reports";
        }
        if (requestUri.matches("^/assignments/\\d+/questions/.*")) {
            return "Question Builder";
        }
        if (requestUri.matches("^/assignments/\\d+$")) {
            return "Assignment Workspace";
        }
        if (requestUri.startsWith("/assignments")) {
            return "Assignments";
        }
        if (requestUri.startsWith("/login")) {
            return "Sign In";
        }
        if (requestUri.startsWith("/search")) {
            return "Search";
        }
        return "Teacher Workspace";
    }

    private String resolvePageTrail(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "WORKBOARD / DASHBOARD";
        }
        if (requestUri.startsWith("/dashboard")) {
            return "WORKBOARD / DASHBOARD";
        }
        if (requestUri.startsWith("/semesters")) {
            return requestUri.contains("/create")
                    ? "DASHBOARD / SEMESTERS / CREATE"
                    : requestUri.matches("^/semesters/\\d+.*$")
                    ? "DASHBOARD / SEMESTERS / DETAIL"
                    : "DASHBOARD / SEMESTERS";
        }
        if (requestUri.startsWith("/courses")) {
            return requestUri.contains("/create")
                    ? "SEMESTERS / COURSES / CREATE"
                    : "SEMESTERS / COURSES";
        }
        if (requestUri.startsWith("/assignments")) {
            if (requestUri.contains("/submissions/upload")) {
                return "SEMESTERS / ASSIGNMENTS / INTAKE";
            }
            if (requestUri.contains("/plagiarism")) {
                return "SEMESTERS / ASSIGNMENTS / PLAGIARISM";
            }
            if (requestUri.contains("/grading")) {
                return "SEMESTERS / ASSIGNMENTS / GRADING";
            }
            if (requestUri.contains("/batches/precheck")) {
                return "SEMESTERS / ASSIGNMENTS / BATCH PRECHECK";
            }
            if (requestUri.contains("/batches/")) {
                return "SEMESTERS / ASSIGNMENTS / BATCH";
            }
            if (requestUri.contains("/results/")) {
                return "SEMESTERS / ASSIGNMENTS / RESULT DETAIL";
            }
            if (requestUri.contains("/results")) {
                return "SEMESTERS / ASSIGNMENTS / RESULTS";
            }
            if (requestUri.contains("/reports")) {
                return "SEMESTERS / ASSIGNMENTS / REPORTS";
            }
            if (requestUri.contains("/questions")) {
                return "SEMESTERS / ASSIGNMENTS / QUESTION BUILDER";
            }
            if (requestUri.contains("/create")) {
                return "SEMESTERS / ASSIGNMENTS / CREATE";
            }
            return "SEMESTERS / ASSIGNMENTS / WORKSPACE";
        }
        if (requestUri.startsWith("/search")) {
            return "WORKBOARD / SEARCH";
        }
        return "WORKBOARD / PAGE";
    }

    private String resolvePrimarySection(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "dashboard";
        }
        if (requestUri.startsWith("/dashboard") || requestUri.startsWith("/search")) {
            return "dashboard";
        }
        if (requestUri.startsWith("/semesters")
                || requestUri.startsWith("/courses")
                || requestUri.startsWith("/assignments")) {
            return "semesters";
        }
        return "user";
    }

    private String resolvePageDescription(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "Teacher workspace for Java assignment setup, plagiarism review, batch grading, and reporting.";
        }
        if (requestUri.startsWith("/login")) {
            return "Secure teacher portal for managing Java grading workflows, from semester setup to batch results and reports.";
        }
        if (requestUri.startsWith("/dashboard")) {
            return "Teacher workboard showing semesters, course progress, assignments needing action, batches, and grading analytics.";
        }
        if (requestUri.startsWith("/semesters")) {
            return "Manage semesters, view courses inside each term, and open assignment workspaces without losing context.";
        }
        if (requestUri.startsWith("/courses")) {
            return "Create and manage teaching courses, weekly structures, and grading spaces inside a semester.";
        }
        if (requestUri.startsWith("/assignments")) {
            if (requestUri.contains("/grading")) {
                return "Freeze the grading snapshot, monitor execution, inspect results, and export teacher reports from one workspace.";
            }
            if (requestUri.contains("/submissions/upload")) {
                return "Upload ZIP submissions, validate intake structure, and prepare assignments for plagiarism and batch grading.";
            }
            if (requestUri.contains("/plagiarism")) {
                return "Review plagiarism signals, blocked submissions, and teacher overrides before freezing a grading batch.";
            }
            if (requestUri.contains("/batches")) {
                return "Freeze a grading snapshot, run the batch pipeline, and monitor execution progress in one place.";
            }
            if (requestUri.contains("/results")) {
                return "Inspect grading results, testcase outcomes, compile logs, and assignment performance after batch execution.";
            }
            if (requestUri.contains("/reports")) {
                return "Generate exportable grade reports, summaries, and result files for completed Java grading batches.";
            }
            return "Teacher assignment workspace for setup, testcase authoring, intake review, plagiarism, and results.";
        }
        if (requestUri.startsWith("/search")) {
            return "Search semesters, courses, and assignments across the JavaGrader teacher workspace.";
        }
        return "Teacher workspace for managing Java grading from setup to reporting.";
    }

    private String resolvePageRobots(String requestUri) {
        if (requestUri != null && requestUri.startsWith("/login")) {
            return "index,follow";
        }
        return "noindex,nofollow";
    }
}
