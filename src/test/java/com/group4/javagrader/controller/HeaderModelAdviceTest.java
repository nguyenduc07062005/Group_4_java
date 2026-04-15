package com.group4.javagrader.controller;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeaderModelAdviceTest {

    private HeaderPageMetadataResolver pageMetadataResolver;

    @Mock
    private SemesterService semesterService;

    @Mock
    private CourseService courseService;

    @Mock
    private AssignmentService assignmentService;

    private HeaderModelAdvice headerModelAdvice;

    @BeforeEach
    void setUp() {
        pageMetadataResolver = new HeaderPageMetadataResolver();
        headerModelAdvice = new HeaderModelAdvice(
                semesterService,
                courseService,
                assignmentService,
                pageMetadataResolver);
    }

    @Test
    void populateHeaderModelUsesBulkSemesterLookupsForCounts() {
        Semester firstSemester = semester(1L, "SP26", "Spring 2026");
        Semester secondSemester = semester(2L, "SU26", "Summer 2026");
        when(semesterService.findActiveSemesters()).thenReturn(List.of(firstSemester, secondSemester));

        when(courseService.findBySemesterIds(List.of(1L, 2L))).thenReturn(List.of(
                course(firstSemester, "SE101"),
                course(firstSemester, "SE102"),
                course(secondSemester, "SE201")));
        when(assignmentService.findBySemesterIds(List.of(1L, 2L))).thenReturn(List.of(
                assignment(firstSemester, "Lab 1"),
                assignment(secondSemester, "Lab 2")));

        Model model = new ExtendedModelMap();
        HttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        Authentication authentication = new TestingAuthenticationToken("teacher", null, "ROLE_TEACHER");

        headerModelAdvice.populateHeaderModel(model, authentication, request);

        verify(courseService).findBySemesterIds(List.of(1L, 2L));
        verify(courseService, never()).findBySemesterId(anyLong());
        verify(assignmentService).findBySemesterIds(List.of(1L, 2L));
        verify(assignmentService, never()).findBySemesterId(anyLong());

        assertThat(model.getAttribute("headerCourseCount")).isEqualTo(3);
        assertThat(model.getAttribute("headerAssignmentCount")).isEqualTo(2);
        assertThat(model.getAttribute("headerSemesterCount")).isEqualTo(2);
    }

    @Test
    void populateHeaderModelSkipsBulkLookupsWhenNoActiveSemesterExists() {
        when(semesterService.findActiveSemesters()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        HttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        Authentication authentication = new TestingAuthenticationToken("teacher", null, "ROLE_TEACHER");

        headerModelAdvice.populateHeaderModel(model, authentication, request);

        verify(courseService, never()).findBySemesterIds(List.of());
        verify(assignmentService, never()).findBySemesterIds(List.of());
        verify(courseService, never()).findBySemesterId(anyLong());
        verify(assignmentService, never()).findBySemesterId(anyLong());

        assertThat(model.getAttribute("headerCourseCount")).isEqualTo(0);
        assertThat(model.getAttribute("headerAssignmentCount")).isEqualTo(0);
        assertThat(model.getAttribute("headerSemesterCount")).isEqualTo(0);
        assertThat(model.getAttribute("headerNotificationCount")).isEqualTo(1);
    }

    @Test
    void populateHeaderModelSkipsDatabaseLookupsForJsonRequests() {
        Model model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        request.addHeader("Accept", "application/json");
        Authentication authentication = new TestingAuthenticationToken("teacher", null, "ROLE_TEACHER");

        headerModelAdvice.populateHeaderModel(model, authentication, request);

        verify(courseService, never()).findBySemesterIds(List.of());
        verify(assignmentService, never()).findBySemesterIds(List.of());
        verify(courseService, never()).findBySemesterId(anyLong());
        verify(assignmentService, never()).findBySemesterId(anyLong());
        verify(semesterService, never()).findActiveSemesters();

        assertThat(model.asMap()).isEmpty();
    }

    private Semester semester(Long id, String code, String name) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setCode(code);
        semester.setName(name);
        return semester;
    }

    private Course course(Semester semester, String courseCode) {
        Course course = new Course();
        course.setSemester(semester);
        course.setCourseCode(courseCode);
        course.setCourseName(courseCode + " Name");
        course.setWeekCount(1);
        return course;
    }

    private Assignment assignment(Semester semester, String title) {
        Assignment assignment = new Assignment();
        assignment.setSemester(semester);
        assignment.setAssignmentName(title);
        return assignment;
    }
}
