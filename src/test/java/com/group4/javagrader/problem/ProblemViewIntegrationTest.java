package com.group4.javagrader.problem;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ProblemViewIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ProblemService problemService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void createProblemRouteReturnsTeachersToAssignmentWorkspace() throws Exception {
        Long semesterId = createSemester("PV26");
        Long assignmentId = createAssignment(semesterId, "Problem Lab");

        mockMvc.perform(get("/problems/create")
                        .param("assignmentId", String.valueOf(assignmentId))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "#setup-workspace"));
    }

    @Test
    void redirectedProblemCreateWorkspaceShowsQuestionCreateFormForFreshAssignment() throws Exception {
        Long semesterId = createSemester("PV27");
        Long assignmentId = createAssignment(semesterId, "Fresh Problem Lab");

        mockMvc.perform(get("/problems/create")
                        .param("assignmentId", String.valueOf(assignmentId))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "#setup-workspace"));

        mockMvc.perform(get("/assignments/{id}", assignmentId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Question details")))
                .andExpect(content().string(containsString("Save Question")))
                .andExpect(content().string(containsString("/assignments/" + assignmentId + "/questions")));
    }

    @Test
    void createProblemRejectsTamperedAssignmentContext() throws Exception {
        Long semesterId = createSemester("PV26");
        Long intendedAssignmentId = createAssignment(semesterId, "Intended Problem Lab");
        Long tamperedAssignmentId = createAssignment(semesterId, "Tampered Problem Lab");

        mockMvc.perform(post("/problems/create")
                        .queryParam("contextAssignmentId", String.valueOf(intendedAssignmentId))
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("assignmentId", String.valueOf(tamperedAssignmentId))
                        .param("title", "Mismatched Problem")
                        .param("maxScore", "10")
                        .param("inputMode", "STDIN")
                        .param("outputComparisonMode", "EXACT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + intendedAssignmentId + "#setup-workspace"));

        org.assertj.core.api.Assertions.assertThat(problemService.findByAssignmentId(intendedAssignmentId)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(problemService.findByAssignmentId(tamperedAssignmentId)).isEmpty();
    }

    private Long createSemester(String codePrefix) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode(codePrefix + "-" + System.nanoTime());
        semesterForm.setName("Semester " + codePrefix);
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
}
