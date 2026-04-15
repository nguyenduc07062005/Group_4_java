package com.group4.javagrader.assignment;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.ProblemRepository;
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
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class AssignmentViewIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private ProblemRepository problemRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void assignmentDetailUsesUnifiedLayoutWithoutOldHeroCopy() throws Exception {
        Long semesterId = createSemester("AV26");
        Long assignmentId = createAssignment(semesterId, "Editable Lab");

        mockMvc.perform(get("/assignments/{id}", assignmentId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Editable Lab")))
                .andExpect(content().string(containsString("Semester Board")))
                .andExpect(content().string(containsString("Assignment Info")))
                .andExpect(content().string(containsString("Default Mark")))
                .andExpect(content().string(containsString("Runtime Defaults")))
                .andExpect(content().string(containsString("Bulk Import")))
                .andExpect(content().string(containsString("Testcases")))
                .andExpect(content().string(containsString("Intake Readiness")))
                .andExpect(content().string(containsString("Stored Files")))
                .andExpect(content().string(containsString("Submission Intake")))
                .andExpect(content().string(containsString("1 question(s) configured")))
                .andExpect(content().string(not(containsString("Assignment Flow"))))
                .andExpect(content().string(not(containsString("Question Setup"))))
                .andExpect(content().string(not(containsString("Create New Question"))))
                .andExpect(content().string(not(containsString("Each assignment is one graded question."))))
                .andExpect(content().string(not(containsString("A teaching week can contain many assignments."))))
                .andExpect(content().string(not(containsString("Create Another Assignment"))));
    }

    @Test
    void editAssignmentFormUsesSharedCreateTemplate() throws Exception {
        Long semesterId = createSemester("AE26");
        Long assignmentId = createAssignment(semesterId, "Config Lab");

        mockMvc.perform(get("/assignments/{id}/edit", assignmentId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/create"))
                .andExpect(content().string(containsString("Edit Assignment")))
                .andExpect(content().string(containsString("Update Assignment")))
                .andExpect(content().string(containsString("Where does this assignment belong?")))
                .andExpect(content().string(containsString("Reference material")))
                .andExpect(content().string(containsString("Teacher Brief")))
                .andExpect(content().string(containsString("Upload .pdf or .md brief for teachers.")))
                .andExpect(content().string(containsString("Input Mode")))
                .andExpect(content().string(containsString("Console / STDIN")))
                .andExpect(content().string(containsString("Read from input.txt")))
                .andExpect(content().string(not(containsString("Display Order"))))
                .andExpect(content().string(containsString("Config Lab")));
    }

    @Test
    void editAssignmentFormUsesHiddenRuntimeDefaultsWhenQuestionsAlreadySplit() throws Exception {
        Long semesterId = createSemester("AE27");
        Long assignmentId = createAssignment(semesterId, "Runtime Lab");
        createVisibleProblem(assignmentId, "Question One", InputMode.STDIN, OutputComparisonMode.EXACT);
        createVisibleProblem(assignmentId, "Question Two", InputMode.FILE, OutputComparisonMode.TRIM_ALL);
        createHiddenRuntimeDefault(assignmentId, "Assignment Runtime Defaults", InputMode.FILE, OutputComparisonMode.TRIM_ALL);

        mockMvc.perform(get("/assignments/{id}/edit", assignmentId).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/create"))
                .andExpect(model().attribute("assignmentForm", hasProperty("inputMode", is(InputMode.FILE))));
    }

    @Test
    void createAssignmentFormCancelReturnsToCurrentSemesterWhenSemesterIsSelected() throws Exception {
        Long semesterId = createSemester("AC26");

        mockMvc.perform(get("/assignments/create")
                        .param("semesterId", String.valueOf(semesterId))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/create"))
                .andExpect(content().string(containsString("/semesters/" + semesterId)))
                .andExpect(content().string(not(containsString(">Back To Dashboard<"))))
                .andExpect(content().string(not(containsString("href=\"/dashboard\">Cancel"))));
    }

    @Test
    void deleteAssignmentRedirectsToSemesterAndRemovesIt() throws Exception {
        Long semesterId = createSemester("AD26");
        Long assignmentId = createAssignment(semesterId, "Delete Lab");

        mockMvc.perform(post("/assignments/{id}/delete", assignmentId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        org.assertj.core.api.Assertions.assertThat(assignmentRepository.findById(assignmentId)).isEmpty();
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

    private void createVisibleProblem(
            Long assignmentId,
            String title,
            InputMode inputMode,
            OutputComparisonMode outputComparisonMode) {
        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle(title);
        problemForm.setMaxScore(java.math.BigDecimal.TEN);
        problemForm.setInputMode(inputMode);
        problemForm.setOutputComparisonMode(outputComparisonMode);
        problemService.create(problemForm);
    }

    private void createHiddenRuntimeDefault(
            Long assignmentId,
            String title,
            InputMode inputMode,
            OutputComparisonMode outputComparisonMode) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        Problem runtimeDefaults = new Problem();
        runtimeDefaults.setAssignment(assignment);
        runtimeDefaults.setProblemOrder(99);
        runtimeDefaults.setTitle(title);
        runtimeDefaults.setMaxScore(java.math.BigDecimal.valueOf(88));
        runtimeDefaults.setInputMode(inputMode);
        runtimeDefaults.setOutputComparisonMode(outputComparisonMode);
        runtimeDefaults.setInternalDefault(true);
        problemRepository.save(runtimeDefaults);
    }
}
