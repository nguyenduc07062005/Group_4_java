package com.group4.javagrader.search;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class GlobalSearchControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssignmentService assignmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void emptyQueryRedirectsToDashboardWithWarning() throws Exception {
        mockMvc.perform(get("/search")
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("warningMessage", "Enter a keyword to search semesters, courses, or assignments."));
    }

    @Test
    void noMatchRedirectsToSemestersWithWarning() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "does-not-exist")
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/semesters"))
                .andExpect(flash().attributeExists("warningMessage"));
    }

    @Test
    void assignmentMatchRedirectsToAssignmentDetail() throws Exception {
        Long semesterId = createSemester("SRCH");
        Long courseId = createCourse(semesterId, "SWE201", "Software Engineering");
        Long assignmentId = createAssignment(semesterId, courseId, "Collections Lab");

        mockMvc.perform(get("/search")
                        .param("q", "Collections")
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId));
    }

    @Test
    void shortEntityQueryShouldNotBeHijackedByStaticShortcut() throws Exception {
        Long semesterId = createSemester("SPRING");
        createCourse(semesterId, "SE101", "Secure Systems");

        MvcResult result = mockMvc.perform(get("/search")
                        .param("q", "se")
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(result.getResponse().getRedirectedUrl())
                .isNotEqualTo("/semesters")
                .isNotEqualTo("/dashboard");
    }

    private Long createSemester(String codePrefix) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode(codePrefix + "-" + System.nanoTime());
        semesterForm.setName("Semester " + codePrefix);
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        return semesterService.create(semesterForm);
    }

    private Long createCourse(Long semesterId, String courseCode, String courseName) {
        CourseForm courseForm = new CourseForm();
        courseForm.setSemesterId(semesterId);
        courseForm.setCourseCode(courseCode);
        courseForm.setCourseName(courseName);
        courseForm.setWeekCount(12);
        courseForm.setCreateWeeklyAssignments(false);
        return courseService.create(courseForm);
    }

    private Long createAssignment(Long semesterId, Long courseId, String assignmentName) {
        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName(assignmentName);
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setCourseId(courseId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(30);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        return assignmentService.create(assignmentForm);
    }
}
