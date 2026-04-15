package com.group4.javagrader.service.impl;

import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.GlobalSearchService;
import com.group4.javagrader.service.SemesterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final SemesterService semesterService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;

    public GlobalSearchServiceImpl(
            SemesterService semesterService,
            CourseService courseService,
            AssignmentService assignmentService) {
        this.semesterService = semesterService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
    }

    @Override
    public String resolveDestination(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return "redirect:/dashboard";
        }

        String needle = normalizedQuery.toLowerCase(Locale.ROOT);
        if ("dashboard".equals(needle)) {
            return "redirect:/dashboard";
        }
        if ("semester".equals(needle) || "semesters".equals(needle)) {
            return "redirect:/semesters";
        }

        Match<Assignment> bestAssignment = null;
        Match<Course> bestCourse = null;
        Match<Semester> bestSemester = null;

        List<Semester> semesters = semesterService.findActiveSemesters();
        for (Semester semester : semesters) {
            int semesterScore = score(needle, semester.getCode(), semester.getName());
            if (semesterScore > 0 && isBetter(semesterScore, bestSemester)) {
                bestSemester = new Match<>(semester, semesterScore);
            }

            List<Course> courses = courseService.findBySemesterId(semester.getId());
            for (Course course : courses) {
                int courseScore = score(needle, course.getCourseCode(), course.getCourseName(), course.getDisplayLabel());
                if (courseScore > 0 && isBetter(courseScore, bestCourse)) {
                    bestCourse = new Match<>(course, courseScore);
                }

                List<Assignment> assignments = assignmentService.findByCourseId(course.getId());
                for (Assignment assignment : assignments) {
                    int assignmentScore = score(
                            needle,
                            assignment.getAssignmentName(),
                            assignment.getAssignmentType().name(),
                            course.getDisplayLabel(),
                            assignment.getAssignmentType() == AssignmentType.WEEKLY && assignment.getWeekNumber() != null
                                    ? "week " + assignment.getWeekNumber()
                                    : null);
                    if (assignmentScore > 0 && isBetter(assignmentScore, bestAssignment)) {
                        bestAssignment = new Match<>(assignment, assignmentScore);
                    }
                }
            }
        }

        if (bestAssignment != null) {
            return "redirect:/assignments/" + bestAssignment.value().getId();
        }

        if (bestCourse != null) {
            Course course = bestCourse.value();
            return "redirect:/semesters/" + course.getSemester().getId()
                    + "?highlightCourseId=" + course.getId()
                    + "#course-" + course.getId();
        }

        if (bestSemester != null) {
            return "redirect:/semesters/" + bestSemester.value().getId();
        }

        return "redirect:/semesters";
    }

    private boolean isBetter(int candidateScore, Match<?> currentBest) {
        return currentBest == null || candidateScore > currentBest.score();
    }

    private int score(String needle, String... fields) {
        int bestScore = 0;
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }

            String normalizedField = field.toLowerCase(Locale.ROOT);
            if (normalizedField.equals(needle)) {
                bestScore = Math.max(bestScore, 100);
            } else if (normalizedField.startsWith(needle)) {
                bestScore = Math.max(bestScore, 80);
            } else if (normalizedField.contains(needle)) {
                bestScore = Math.max(bestScore, 60);
            }
        }
        return bestScore;
    }

    private record Match<T>(T value, int score) {
    }
}
