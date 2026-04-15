package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AssignmentGroupView;
import com.group4.javagrader.dto.CourseBoardView;
import com.group4.javagrader.dto.SemesterBoardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.TeacherBoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TeacherBoardServiceImpl implements TeacherBoardService {

    private final SemesterService semesterService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;

    public TeacherBoardServiceImpl(
            SemesterService semesterService,
            CourseService courseService,
            AssignmentService assignmentService) {
        this.semesterService = semesterService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemesterBoardView> buildActiveSemesterBoard() {
        List<Semester> semesters = semesterService.findActiveSemesters();
        return buildBoardViews(semesters);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SemesterBoardView> buildSemesterBoard(Long semesterId) {
        return semesterService.findActiveById(semesterId)
                .map(semester -> buildBoardViews(List.of(semester)).get(0));
    }

    private List<SemesterBoardView> buildBoardViews(List<Semester> semesters) {
        if (semesters.isEmpty()) {
            return List.of();
        }

        List<Long> semesterIds = semesters.stream()
                .map(Semester::getId)
                .toList();
        Map<Long, List<Course>> coursesBySemesterId = courseService.findBySemesterIds(semesterIds).stream()
                .collect(Collectors.groupingBy(
                        course -> course.getSemester().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<Assignment>> assignmentsByCourseId = assignmentService.findBySemesterIds(semesterIds).stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getCourse().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return semesters.stream()
                .map(semester -> toBoardView(
                        semester,
                        coursesBySemesterId.getOrDefault(semester.getId(), List.of()),
                        assignmentsByCourseId))
                .toList();
    }

    private SemesterBoardView toBoardView(
            Semester semester,
            List<Course> courses,
            Map<Long, List<Assignment>> assignmentsByCourseId) {
        List<CourseBoardView> courseViews = courses.stream()
                .map(course -> toCourseView(course, assignmentsByCourseId.getOrDefault(course.getId(), List.of())))
                .toList();
        int assignmentCount = courseViews.stream()
                .mapToInt(CourseBoardView::getAssignmentCount)
                .sum();
        return new SemesterBoardView(semester, courseViews, assignmentCount);
    }

    private CourseBoardView toCourseView(Course course, List<Assignment> assignments) {
        List<AssignmentGroupView> groups = new ArrayList<>();

        Map<Integer, List<Assignment>> weeklyAssignments = new LinkedHashMap<>();
        List<Assignment> customAssignments = new ArrayList<>();
        List<Assignment> specialAssignments = new ArrayList<>();

        for (Assignment assignment : assignments) {
            if (assignment.isWeeklyAssignmentType() && assignment.getWeekNumber() != null) {
                weeklyAssignments.computeIfAbsent(assignment.getWeekNumber(), ignored -> new ArrayList<>()).add(assignment);
                continue;
            }
            if (assignment.isCustomAssignmentType()) {
                customAssignments.add(assignment);
                continue;
            }
            specialAssignments.add(assignment);
        }

        weeklyAssignments.forEach((weekNumber, weeklyGroup) -> groups.add(new AssignmentGroupView(
                "Week " + weekNumber,
                weeklyGroup.size() == 1
                        ? "1 assignment ready in this teaching week."
                        : weeklyGroup.size() + " assignments are grouped in this teaching week.",
                "calendar_view_week",
                weekNumber,
                weeklyGroup)));

        if (!customAssignments.isEmpty()) {
            groups.add(new AssignmentGroupView(
                    "Custom Assignments",
                    "Standalone grading workspaces outside the weekly track.",
                    "tune",
                    null,
                    customAssignments));
        }

        if (!specialAssignments.isEmpty()) {
            groups.add(new AssignmentGroupView(
                    "Special Assignments",
                    "Legacy or special-purpose setups still available in this course.",
                    "star",
                    null,
                    specialAssignments));
        }

        return new CourseBoardView(course, groups, assignments.size());
    }
}
