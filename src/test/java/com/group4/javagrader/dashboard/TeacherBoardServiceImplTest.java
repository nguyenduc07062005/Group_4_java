package com.group4.javagrader.dashboard;

import com.group4.javagrader.dto.SemesterBoardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.impl.TeacherBoardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherBoardServiceImplTest {

    @Mock
    private SemesterService semesterService;

    @Mock
    private CourseService courseService;

    @Mock
    private AssignmentService assignmentService;

    private TeacherBoardServiceImpl teacherBoardService;

    @BeforeEach
    void setUp() {
        teacherBoardService = new TeacherBoardServiceImpl(semesterService, courseService, assignmentService);
    }

    @Test
    void buildActiveSemesterBoardUsesBulkSemesterLookupsAndPreservesGrouping() {
        Semester firstSemester = semester(1L, "SP26", "Spring 2026");
        Semester secondSemester = semester(2L, "SU26", "Summer 2026");

        Course alphaCourse = course(11L, firstSemester, "SE101", "Software Engineering");
        Course betaCourse = course(12L, firstSemester, "PRJ301", "Project Lab");
        Course gammaCourse = course(21L, secondSemester, "MAD101", "Discrete Math");

        Assignment weekOne = assignment(101L, firstSemester, alphaCourse, "Week 1 Lab", AssignmentType.WEEKLY, 1, 101);
        Assignment weekOneExtra = assignment(102L, firstSemester, alphaCourse, "Week 1 Quiz", AssignmentType.WEEKLY, 1, 102);
        Assignment custom = assignment(103L, firstSemester, alphaCourse, "Midterm Practice", AssignmentType.CUSTOM, null, 1000);
        Assignment intro = assignment(104L, firstSemester, betaCourse, "Intro Setup", AssignmentType.INTRO, null, 0);
        Assignment weeklySummer = assignment(201L, secondSemester, gammaCourse, "Week 2 Lab", AssignmentType.WEEKLY, 2, 102);

        when(semesterService.findActiveSemesters()).thenReturn(List.of(firstSemester, secondSemester));
        when(courseService.findBySemesterIds(List.of(1L, 2L))).thenReturn(List.of(alphaCourse, betaCourse, gammaCourse));
        when(assignmentService.findBySemesterIds(List.of(1L, 2L)))
                .thenReturn(List.of(weekOne, weekOneExtra, custom, intro, weeklySummer));

        List<SemesterBoardView> board = teacherBoardService.buildActiveSemesterBoard();

        verify(courseService).findBySemesterIds(List.of(1L, 2L));
        verify(assignmentService).findBySemesterIds(List.of(1L, 2L));
        verify(courseService, never()).findBySemesterId(1L);
        verify(courseService, never()).findBySemesterId(2L);
        verify(assignmentService, never()).findByCourseId(alphaCourse.getId());
        verify(assignmentService, never()).findByCourseId(betaCourse.getId());
        verify(assignmentService, never()).findByCourseId(gammaCourse.getId());

        assertThat(board).hasSize(2);
        assertThat(board.get(0).getSemester().getId()).isEqualTo(1L);
        assertThat(board.get(0).getCourses()).hasSize(2);
        assertThat(board.get(0).getAssignmentCount()).isEqualTo(4);
        assertThat(board.get(1).getSemester().getId()).isEqualTo(2L);
        assertThat(board.get(1).getCourses()).hasSize(1);
        assertThat(board.get(1).getAssignmentCount()).isEqualTo(1);

        assertThat(board.get(0).getCourses().get(0).getCourse().getId()).isEqualTo(alphaCourse.getId());
        assertThat(board.get(0).getCourses().get(0).getAssignmentGroups())
                .extracting(group -> group.getLabel())
                .containsExactly("Week 1", "Custom Assignments");
        assertThat(board.get(0).getCourses().get(0).getAssignmentGroups().get(0).getAssignments())
                .extracting(Assignment::getId)
                .containsExactly(101L, 102L);
        assertThat(board.get(0).getCourses().get(1).getAssignmentGroups())
                .extracting(group -> group.getLabel())
                .containsExactly("Special Assignments");
        assertThat(board.get(1).getCourses().get(0).getAssignmentGroups())
                .extracting(group -> group.getLabel())
                .containsExactly("Week 2");
    }

    @Test
    void buildSemesterBoardUsesBulkLoadingForSingleSemester() {
        Semester semester = semester(1L, "SP26", "Spring 2026");
        Course course = course(11L, semester, "SE101", "Software Engineering");
        Assignment assignment = assignment(101L, semester, course, "Week 3 Lab", AssignmentType.WEEKLY, 3, 103);

        when(semesterService.findActiveById(1L)).thenReturn(Optional.of(semester));
        when(courseService.findBySemesterIds(List.of(1L))).thenReturn(List.of(course));
        when(assignmentService.findBySemesterIds(List.of(1L))).thenReturn(List.of(assignment));

        Optional<SemesterBoardView> board = teacherBoardService.buildSemesterBoard(1L);

        assertThat(board).isPresent();
        assertThat(board.get().getCourses()).hasSize(1);
        assertThat(board.get().getCourses().get(0).getAssignmentGroups())
                .extracting(group -> group.getLabel())
                .containsExactly("Week 3");
        verify(courseService).findBySemesterIds(List.of(1L));
        verify(assignmentService).findBySemesterIds(List.of(1L));
        verify(courseService, never()).findBySemesterId(1L);
        verify(assignmentService, never()).findByCourseId(course.getId());
    }

    private Semester semester(Long id, String code, String name) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setCode(code);
        semester.setName(name);
        semester.setStartDate(LocalDate.of(2026, 1, 6));
        semester.setEndDate(LocalDate.of(2026, 5, 30));
        return semester;
    }

    private Course course(Long id, Semester semester, String courseCode, String courseName) {
        Course course = new Course();
        course.setId(id);
        course.setSemester(semester);
        course.setCourseCode(courseCode);
        course.setCourseName(courseName);
        course.setWeekCount(12);
        course.setArchived(false);
        return course;
    }

    private Assignment assignment(
            Long id,
            Semester semester,
            Course course,
            String name,
            AssignmentType assignmentType,
            Integer weekNumber,
            int displayOrder) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setSemester(semester);
        assignment.setCourse(course);
        assignment.setAssignmentName(name);
        assignment.setAssignmentType(assignmentType);
        assignment.setWeekNumber(weekNumber);
        assignment.setDisplayOrder(displayOrder);
        assignment.setGradingMode(GradingMode.JAVA_CORE);
        assignment.setPlagiarismThreshold(BigDecimal.valueOf(80));
        assignment.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);
        return assignment;
    }
}
