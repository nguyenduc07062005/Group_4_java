package com.group4.javagrader.testcase;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.GradingResultStatus;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.ProblemResult;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.entity.TestCaseResult;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.ProblemResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.TestCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class PhaseTwoViewIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private GradingResultRepository gradingResultRepository;

    @Autowired
    private ProblemResultRepository problemResultRepository;

    @Autowired
    private TestCaseResultRepository testCaseResultRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void assignmentDetailRendersPhaseTwoSections() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId()).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Setup Status")))
                .andExpect(content().string(containsString("Default Mark")))
                .andExpect(content().string(containsString("Selected Question")))
                .andExpect(content().string(containsString("Intake Readiness")))
                .andExpect(content().string(containsString("Existing Testcases")))
                .andExpect(content().string(not(containsString("Assignment Flow"))))
                .andExpect(content().string(not(containsString("Question Setup"))))
                .andExpect(content().string(not(containsString("Workspace Status"))));
    }

    @Test
    void testcaseCreateRouteReturnsTeachersToAssignmentWorkspace() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(get("/problems/{problemId}/testcases/create", ids.problemId()).with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab"));
    }

    @Test
    void testcaseCreateRouteStillReturnsTeachersToAssignmentWorkspaceWhenAllProblemsAreReady() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);

        TestCaseForm secondProblemTestCase = new TestCaseForm();
        secondProblemTestCase.setProblemId(ids.secondProblemId());
        secondProblemTestCase.setInputData("5");
        secondProblemTestCase.setExpectedOutput("5");
        secondProblemTestCase.setWeight(BigDecimal.ONE);
        testCaseService.create(secondProblemTestCase);

        mockMvc.perform(get("/problems/{problemId}/testcases/create", ids.problemId()).with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab"));
    }

    @Test
    void assignmentWorkspaceShowsEditAffordancesForSelectedProblemAndTestcases() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);
        Long testCaseId = testCaseService.findByProblemId(ids.problemId()).get(0).getId();

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId())
                        .param("problemId", String.valueOf(ids.problemId()))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Edit Question")))
                .andExpect(content().string(containsString("Save Testcase")))
                .andExpect(content().string(containsString("Question details")))
                .andExpect(content().string(containsString("Current evidence for this question")))
                .andExpect(content().string(not(containsString("Update Question"))))
                .andExpect(content().string(not(containsString("Update Testcase"))))
                .andExpect(content().string(containsString("/assignments/" + ids.assignmentId() + "/questions/" + ids.problemId() + "/edit")))
                .andExpect(content().string(containsString("/assignments/" + ids.assignmentId() + "/questions/" + ids.problemId() + "/testcases/" + testCaseId + "/edit")));
    }

    @Test
    void updateSelectedProblemRedirectsBackToWorkspace() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}", ids.assignmentId(), ids.problemId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("assignmentId", String.valueOf(ids.assignmentId()))
                        .param("title", "Collections Warmup Updated")
                        .param("maxScore", "25")
                        .param("inputMode", "FILE")
                        .param("outputComparisonMode", "TRIM_ALL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab"));
    }

    @Test
    void updateSelectedTestcaseRedirectsBackToWorkspace() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);

        Long testCaseId = testCaseService.findByProblemId(ids.problemId()).get(0).getId();

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases/{testCaseId}", ids.assignmentId(), ids.problemId(), testCaseId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("problemId", String.valueOf(ids.problemId()))
                        .param("inputData", "9 9")
                        .param("expectedOutput", "18")
                        .param("weight", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab"));
    }

    @Test
    void creatingTestcaseRejectsProblemFromAnotherAssignment() throws Exception {
        PhaseTwoIds ownerIds = createAssignmentChain(false);
        PhaseTwoIds routeIds = createAssignmentChain(false);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases", routeIds.assignmentId(), ownerIds.problemId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("problemId", String.valueOf(ownerIds.problemId()))
                        .param("inputData", "7")
                        .param("expectedOutput", "7")
                        .param("weight", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/semesters"));

        assertThat(testCaseService.findByProblemId(ownerIds.problemId())).isEmpty();
    }

    @Test
    void invalidProblemEditRedisplaysWorkspaceWithValidationErrors() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}", ids.assignmentId(), ids.problemId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("assignmentId", String.valueOf(ids.assignmentId()))
                        .param("title", "")
                        .param("maxScore", "25")
                        .param("inputMode", "FILE")
                        .param("outputComparisonMode", "TRIM_ALL"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Problem title is required.")));
    }

    @Test
    void invalidTestcaseEditRedisplaysWorkspaceWithValidationErrors() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);

        Long testCaseId = testCaseService.findByProblemId(ids.problemId()).get(0).getId();

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases/{testCaseId}", ids.assignmentId(), ids.problemId(), testCaseId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("problemId", String.valueOf(ids.problemId()))
                        .param("inputData", "")
                        .param("expectedOutput", "")
                        .param("weight", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Expected output is required.")));
    }

    @Test
    void invalidImportedPreviewRedisplaysWorkspaceWithPreviewValidationErrors() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases/import/save", ids.assignmentId(), ids.problemId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("problemId", String.valueOf(ids.problemId()))
                        .param("rows[0].sourceName", "case-01")
                        .param("rows[0].inputData", "1 2")
                        .param("rows[0].expectedOutput", "")
                        .param("rows[0].weight", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Review the imported rows before saving.")))
                .andExpect(content().string(containsString("Expected output is required.")))
                .andExpect(content().string(containsString("Weight must be greater than 0.")));
    }

    @Test
    void emptyImportedPreviewRedisplaysWorkspaceWithVisibleError() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases/import/save", ids.assignmentId(), ids.problemId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf())
                        .param("problemId", String.valueOf(ids.problemId())))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Import preview is empty.")));
    }

    @Test
    void editingUnknownProblemRedirectsTeachersToSemesters() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(get("/assignments/{assignmentId}/questions/{problemId}/edit", ids.assignmentId(), 999999L)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/semesters"));
    }

    @Test
    void editingUnknownTestcaseRedirectsTeachersToSemesters() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(get("/assignments/{assignmentId}/questions/{problemId}/testcases/{testCaseId}/edit", ids.assignmentId(), ids.problemId(), 999999L)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/semesters"));
    }

    @Test
    void workspaceDeleteActionsRenderAndTestcaseDeleteRedirectsBackToWorkspace() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);
        Long testCaseId = testCaseService.findByProblemId(ids.problemId()).get(0).getId();

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId())
                        .param("problemId", String.valueOf(ids.problemId()))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Delete Question")))
                .andExpect(content().string(containsString("/assignments/" + ids.assignmentId() + "/questions/" + ids.problemId() + "/delete")))
                .andExpect(content().string(containsString("/assignments/" + ids.assignmentId() + "/questions/" + ids.problemId() + "/testcases/" + testCaseId + "/delete")));

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases/{testCaseId}/delete", ids.assignmentId(), ids.problemId(), testCaseId)
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab"));
    }

    @Test
    void assignmentWorkspaceRendersInlineTestcaseEditorHooks() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);
        Long testCaseId = testCaseService.findByProblemId(ids.problemId()).get(0).getId();

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId())
                        .param("problemId", String.valueOf(ids.problemId()))
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("assignmentWorkspaceTestcaseLab(")))
                .andExpect(content().string(containsString("data-testcase-url-base=\"/assignments/" + ids.assignmentId() + "/questions/" + ids.problemId() + "/testcases\"")))
                .andExpect(content().string(containsString("data-testcase-id=\"" + testCaseId + "\"")))
                .andExpect(content().string(not(containsString("/testcases/testcaseId/"))))
                .andExpect(content().string(not(containsString("'}]}{"))));
    }

    @Test
    void deleteInlineTestcaseReturnsFriendlyErrorWhenGradingResultsExist() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);
        TestCase testCase = testCaseService.findByProblemId(ids.problemId()).get(0);
        createHistoricalTestCaseResult(ids.assignmentId(), ids.problemId(), testCase);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/testcases/{testCaseId}/delete-inline",
                        ids.assignmentId(), ids.problemId(), testCase.getId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete testcase after grading results exist."));
    }

    @Test
    void deletingProblemWithTestcasesShowsSafeErrorAndReturnsToWorkspace() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);

        mockMvc.perform(post("/assignments/{assignmentId}/questions/{problemId}/delete", ids.assignmentId(), ids.problemId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab"));
    }

    @Test
    void workspaceSettingsFormUsesHiddenRuntimeDefaultsWhenQuestionsAlreadySplit() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);
        createHiddenRuntimeDefault(
                ids.assignmentId(),
                "Assignment Runtime Defaults",
                BigDecimal.valueOf(88),
                InputMode.FILE,
                OutputComparisonMode.TRIM_ALL);

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId())
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(model().attribute("assignmentForm", hasProperty("defaultMark", comparesEqualTo(BigDecimal.valueOf(88)))))
                .andExpect(model().attribute("assignmentForm", hasProperty("inputMode", is(InputMode.FILE))));
    }

    @Test
    void assignmentDetailShowsMultiQuestionCopyAndCoverageState() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(true);

        TestCaseForm secondProblemTestCase = new TestCaseForm();
        secondProblemTestCase.setProblemId(ids.secondProblemId());
        secondProblemTestCase.setInputData("9");
        secondProblemTestCase.setExpectedOutput("9");
        secondProblemTestCase.setWeight(BigDecimal.ONE);
        testCaseService.create(secondProblemTestCase);

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId()).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("2 question(s) configured")))
                .andExpect(content().string(containsString("All configured questions have testcase coverage.")))
                .andExpect(content().string(not(containsString("Each assignment is one graded question."))))
                .andExpect(content().string(not(containsString("The assignment question has enough testcase coverage."))))
                .andExpect(content().string(not(containsString("Add testcase coverage for the assignment question before intake."))));
    }

    @Test
    void assignmentWorkspaceLinksToEveryConfiguredQuestion() throws Exception {
        PhaseTwoIds ids = createAssignmentChain(false);

        mockMvc.perform(get("/assignments/{id}", ids.assignmentId()).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Choose a question to configure")))
                .andExpect(content().string(containsString("Collections Warmup")))
                .andExpect(content().string(containsString("Collections Advanced")))
                .andExpect(content().string(containsString("/assignments/" + ids.assignmentId() + "?problemId=" + ids.problemId() + "#testcase-lab")))
                .andExpect(content().string(containsString("/assignments/" + ids.assignmentId() + "?problemId=" + ids.secondProblemId() + "#testcase-lab")));
    }

    private PhaseTwoIds createAssignmentChain(boolean includeMarkdownDescription) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode("SP26-P2-" + System.nanoTime());
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Lab Phase 2");
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        assignmentForm.setPlagiarismThreshold(35);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.TRIM_ALL);
        assignmentForm.setLogicWeight(70);
        assignmentForm.setOopWeight(30);
        Long assignmentId = assignmentService.create(assignmentForm);

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Collections Warmup");
        problemForm.setMaxScore(BigDecimal.valueOf(15));
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long problemId = problemService.create(problemForm);

        ProblemForm secondProblemForm = new ProblemForm();
        secondProblemForm.setAssignmentId(assignmentId);
        secondProblemForm.setTitle("Collections Advanced");
        secondProblemForm.setMaxScore(BigDecimal.valueOf(20));
        secondProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.FILE);
        secondProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.TRIM_ALL);
        Long secondProblemId = problemService.create(secondProblemForm);

        if (includeMarkdownDescription) {
            TestCaseForm testCaseForm = new TestCaseForm();
            testCaseForm.setProblemId(problemId);
            testCaseForm.setInputData("1 2");
            testCaseForm.setExpectedOutput("3");
            testCaseForm.setWeight(BigDecimal.valueOf(2));
            testCaseService.create(testCaseForm);
        }

        return new PhaseTwoIds(assignmentId, problemId, secondProblemId);
    }

    private record PhaseTwoIds(Long assignmentId, Long problemId, Long secondProblemId) {
    }

    private void createHiddenRuntimeDefault(
            Long assignmentId,
            String title,
            BigDecimal maxScore,
            InputMode inputMode,
            OutputComparisonMode outputComparisonMode) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        Problem runtimeDefaults = new Problem();
        runtimeDefaults.setAssignment(assignment);
        runtimeDefaults.setProblemOrder(99);
        runtimeDefaults.setTitle(title);
        runtimeDefaults.setMaxScore(maxScore);
        runtimeDefaults.setInputMode(inputMode);
        runtimeDefaults.setOutputComparisonMode(outputComparisonMode);
        runtimeDefaults.setInternalDefault(true);
        problemRepository.save(runtimeDefaults);
    }

    private void createHistoricalTestCaseResult(Long assignmentId, Long problemId, TestCase testCase) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        Problem problem = problemRepository.findById(problemId).orElseThrow();

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setSubmitterName("student01");
        submission.setArchiveFileName("student01.zip");
        submission.setStatus(SubmissionStatus.VALIDATED);
        submission.setFileCount(1);
        submission = submissionRepository.save(submission);

        Batch batch = new Batch();
        batch.setAssignment(assignment);
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setQueueCapacity(1);
        batch.setWorkerCount(1);
        batch.setTotalSubmissions(1);
        batch.setGradeableSubmissionCount(1);
        batch.setExcludedSubmissionCount(0);
        batch = batchRepository.save(batch);

        GradingResult gradingResult = new GradingResult();
        gradingResult.setBatch(batch);
        gradingResult.setSubmission(submission);
        gradingResult.setStatus(GradingResultStatus.DONE);
        gradingResult.setGradingMode(assignment.getGradingMode());
        gradingResult.setTotalScore(testCase.getWeight());
        gradingResult.setMaxScore(problem.getMaxScore());
        gradingResult.setLogicScore(testCase.getWeight());
        gradingResult.setOopScore(BigDecimal.ZERO);
        gradingResult.setTestcasePassedCount(1);
        gradingResult.setTestcaseTotalCount(1);
        gradingResult = gradingResultRepository.save(gradingResult);

        ProblemResult problemResult = new ProblemResult();
        problemResult.setGradingResult(gradingResult);
        problemResult.setProblem(problem);
        problemResult.setStatus("DONE");
        problemResult.setEarnedScore(testCase.getWeight());
        problemResult.setMaxScore(problem.getMaxScore());
        problemResult.setTestcasePassedCount(1);
        problemResult.setTestcaseTotalCount(1);
        problemResult = problemResultRepository.save(problemResult);

        TestCaseResult testCaseResult = new TestCaseResult();
        testCaseResult.setProblemResult(problemResult);
        testCaseResult.setTestCase(testCase);
        testCaseResult.setStatus("PASS");
        testCaseResult.setPassed(true);
        testCaseResult.setEarnedScore(testCase.getWeight());
        testCaseResult.setConfiguredWeight(testCase.getWeight());
        testCaseResult.setExpectedOutput(testCase.getExpectedOutput());
        testCaseResult.setActualOutput(testCase.getExpectedOutput());
        testCaseResultRepository.save(testCaseResult);
    }
}
