package com.group4.javagrader.semester;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.AssignmentService;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class SemesterViewIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void createSemesterRedirectsToSemesterDetailInsteadOfAssignmentForm() throws Exception {
        String code = "SP26-VIEW-" + System.nanoTime();

        MvcResult result = mockMvc.perform(post("/semesters/create")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("code", code)
                        .param("name", "Semester Navigation")
                        .param("startDate", "2026-01-06")
                        .param("endDate", "2026-05-30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Semester semester = semesterRepository.findByArchivedFalseOrderByStartDateDescIdDesc().stream()
                .filter(candidate -> code.equals(candidate.getCode()))
                .findFirst()
                .orElseThrow();

        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/semesters/" + semester.getId());
    }

    @Test
    void dashboardUsesSemesterEntryPointInsteadOfDirectAssignmentForm() throws Exception {
        Long semesterId = createSemester("SP26-DASH");
        createAssignment(semesterId, "Navigation Lab");

        mockMvc.perform(get("/dashboard").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("All your academic workspaces")))
                .andExpect(content().string(containsString("Need Attention")))
                .andExpect(content().string(containsString("Assignments needing action.")))
                .andExpect(content().string(not(containsString("Dashboard Logic"))))
                .andExpect(content().string(not(containsString("Go To Assignment Form"))));
    }

    @Test
    void semesterOverviewShowsCoursesOnlyInsideEachSemesterCard() throws Exception {
        String code = "SP26-BOARD-" + System.nanoTime();
        Long semesterId = createSemester(code);
        createAssignment(semesterId, "Inline Lab");

        mockMvc.perform(get("/semesters").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("semester/index"))
                .andExpect(content().string(containsString(code)))
                .andExpect(content().string(containsString("Semester board")))
                .andExpect(content().string(containsString("GENERAL | Mon hoc chung")))
                .andExpect(content().string(not(containsString("Open Course"))))
                .andExpect(content().string(not(containsString("Tap to open course detail"))))
                .andExpect(content().string(not(containsString("Inline Lab"))))
                .andExpect(content().string(not(containsString("item(s)"))));
    }

    @Test
    void semesterDetailShowsAssignmentsAndContextualCreateAction() throws Exception {
        String code = "SP26-DETAIL-" + System.nanoTime();
        Long semesterId = createSemester(code);
        createAssignment(semesterId, "Phase 4 Warmup");

        mockMvc.perform(get("/semesters/{id}", semesterId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("semester/detail"))
                .andExpect(content().string(containsString(code)))
                .andExpect(content().string(containsString("Courses and assignments")))
                .andExpect(content().string(containsString("New Assignment")))
                .andExpect(content().string(containsString("Edit Semester")))
                .andExpect(content().string(containsString("Delete Semester")))
                .andExpect(content().string(containsString("Phase 4 Warmup")))
                .andExpect(content().string(not(containsString("Select one course to see its week structure across the page."))));
    }

    @Test
    void semesterDetailIgnoresHighlightCourseIdFromAnotherSemester() throws Exception {
        Long semesterId = createSemester("SP26-HL1");
        Long otherSemesterId = createSemester("SP26-HL2");
        createAssignment(semesterId, "Local Lab");
        createAssignment(otherSemesterId, "Foreign Lab");

        Long localCourseId = findGeneralCourseId(semesterId);
        Long foreignCourseId = findGeneralCourseId(otherSemesterId);

                mockMvc.perform(get("/semesters/{id}", semesterId)
                        .param("highlightCourseId", String.valueOf(foreignCourseId))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("activeCourse: &#39;course-" + localCourseId + "&#39;")))
                .andExpect(content().string(not(containsString("activeCourse: &#39;course-" + foreignCourseId + "&#39;"))));
    }

    @Test
    void editSemesterUpdatesValuesAndRedirectsBackToDetail() throws Exception {
        Long semesterId = createSemester("SP26-EDIT");
        String updatedCode = "SU26-EDIT-" + System.nanoTime();

        mockMvc.perform(post("/semesters/{id}/edit", semesterId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("code", updatedCode)
                        .param("name", "Summer 2026")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().is3xxRedirection());

        Semester updated = semesterRepository.findById(semesterId).orElseThrow();
        assertThat(updated.getCode()).isEqualTo(updatedCode);
        assertThat(updated.getName()).isEqualTo("Summer 2026");
        assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));

        mockMvc.perform(get("/semesters/{id}", semesterId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(updatedCode)))
                .andExpect(content().string(containsString("Summer 2026")));
    }

    @Test
    void deleteSemesterArchivesItAndRemovesItFromOverview() throws Exception {
        Long semesterId = createSemester("SP26-DELETE");
        Semester semester = semesterRepository.findById(semesterId).orElseThrow();

        mockMvc.perform(post("/semesters/{id}/delete", semesterId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Semester archived = semesterRepository.findById(semesterId).orElseThrow();
        assertThat(archived.isArchived()).isTrue();

        mockMvc.perform(get("/semesters").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(semester.getCode()))));

        mockMvc.perform(get("/semesters/{id}", semesterId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void deleteSemesterRejectsArchivingWhenAssignmentsStillExist() throws Exception {
        Long semesterId = createSemester("SP26-BLOCK");
        createAssignment(semesterId, "Still Active");

        mockMvc.perform(post("/semesters/{id}/delete", semesterId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Cannot archive a semester that still has active courses or assignments."));

        Semester semester = semesterRepository.findById(semesterId).orElseThrow();
        assertThat(semester.isArchived()).isFalse();

        mockMvc.perform(get("/semesters/{id}", semesterId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Still Active")));
    }

    private Long createSemester(String codePrefix) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode(codePrefix + "-" + System.nanoTime());
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        return semesterService.create(semesterForm);
    }

    private Long createAssignment(Long semesterId, String assignmentName) {
        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName(assignmentName);
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(30);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        return assignmentService.create(assignmentForm);
    }

    private Long findGeneralCourseId(Long semesterId) {
        return courseRepository.findBySemesterIdAndCourseCodeIgnoreCaseAndArchivedFalse(semesterId, "GENERAL")
                .map(Course::getId)
                .orElseThrow();
    }
}
