package com.group4.javagrader.plagiarism;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.PlagiarismService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.SubmissionService;
import com.group4.javagrader.service.TestCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class PhaseFourViewIntegrationTest {

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
    private SubmissionService submissionService;

    @Autowired
    private PlagiarismService plagiarismService;

    @Autowired
    private BatchService batchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void assignmentDetailShowsPhaseFourActions() throws Exception {
        Long assignmentId = createAssignment();
        createProblemAndTestcase(assignmentId);
        uploadAndRunReport(assignmentId);

        mockMvc.perform(get("/assignments/{assignmentId}", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Plagiarism Review")))
                .andExpect(content().string(containsString("Grading Workspace")));
    }

    @Test
    void plagiarismPageRendersPairwiseResultsAndBlockedSubmissions() throws Exception {
        Long assignmentId = createAssignment();
        uploadAndRunReport(assignmentId);

        mockMvc.perform(get("/assignments/{assignmentId}/plagiarism", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("plagiarism/index"))
                .andExpect(content().string(containsString("Plagiarism Review")))
                .andExpect(content().string(containsString("Submission pairs")))
                .andExpect(content().string(containsString("Blocked Submissions")))
                .andExpect(content().string(containsString("Run or review plagiarism before grading")))
                .andExpect(content().string(not(containsString("Open Grading Workspace"))))
                .andExpect(content().string(containsString("s2211001")));
    }

    @Test
    void legacyBatchPrecheckRouteRedirectsToGradingWorkspace() throws Exception {
        Long assignmentId = createAssignment();
        uploadAndRunReport(assignmentId);

        mockMvc.perform(get("/assignments/{assignmentId}/batches/precheck", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "/grading"));
    }

    @Test
    void gradingWorkspaceShowsSnapshotResultsAndExportsAfterBatchStarts() throws Exception {
        Long assignmentId = createAssignment();
        uploadAndRunReport(assignmentId);
        Batch batch = batchService.createBatch(assignmentId, "teacher");
        batchService.startBatch(assignmentId, batch.getId(), "teacher");

        mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(containsString("Snapshot And Run")))
                .andExpect(content().string(containsString("Stored grading output")))
                .andExpect(content().string(containsString("Teacher-facing outputs")))
                .andExpect(content().string(containsString("View Results")))
                .andExpect(content().string(not(containsString("Export Reports"))));
    }

    @Test
    void batchStartRejectsBatchFromAnotherAssignmentRoute() throws Exception {
        Long firstAssignmentId = createAssignment();
        Long secondAssignmentId = createAssignment();
        uploadAndRunReport(secondAssignmentId);
        Batch batch = batchService.createBatch(secondAssignmentId, "teacher");

        mockMvc.perform(post("/assignments/{assignmentId}/batches/{batchId}/start", firstAssignmentId, batch.getId())
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + firstAssignmentId + "/grading#snapshot-run"));

        Batch persistedBatch = batchService.findById(batch.getId()).orElseThrow();
        assertThat(persistedBatch.getAssignment().getId()).isEqualTo(secondAssignmentId);
        assertThat(persistedBatch.getStatus()).isEqualTo(BatchStatus.PRECHECKED);
    }

    @Test
    void plagiarismOverrideRejectsPairFromAnotherAssignmentRoute() throws Exception {
        Long firstAssignmentId = createAssignment();
        Long secondAssignmentId = createAssignment();
        uploadAndRunReport(secondAssignmentId);

        Long reportId = plagiarismService.findLatestReport(secondAssignmentId)
                .orElseThrow()
                .getId();
        Long pairId = plagiarismService.findPairsByReportId(reportId).stream()
                .filter(pair -> pair.isEffectivelyBlocked())
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/assignments/{assignmentId}/plagiarism/pairs/{pairId}/override", firstAssignmentId, pairId)
                        .param("decision", "ALLOW")
                        .param("note", "Cross-assignment attempt")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + firstAssignmentId + "/plagiarism"));

        List<com.group4.javagrader.entity.PlagiarismPair> refreshedPairs = plagiarismService.findPairsByReportId(reportId);
        assertThat(refreshedPairs)
                .filteredOn(pair -> pair.getId().equals(pairId))
                .singleElement()
                .matches(pair -> pair.getOverrideDecision() == null);
    }

    @Test
    void plagiarismOverrideRejectsPairFromOlderReportAfterRerun() throws Exception {
        Long assignmentId = createAssignment();
        uploadAndRunReport(assignmentId);

        Long firstReportId = plagiarismService.findLatestReport(assignmentId)
                .orElseThrow()
                .getId();
        Long oldPairId = plagiarismService.findPairsByReportId(firstReportId).stream()
                .filter(pair -> pair.isEffectivelyBlocked())
                .findFirst()
                .orElseThrow()
                .getId();

        plagiarismService.runReport(assignmentId, "teacher");

        mockMvc.perform(post("/assignments/{assignmentId}/plagiarism/pairs/{pairId}/override", assignmentId, oldPairId)
                        .param("decision", "ALLOW")
                        .param("note", "Stale report attempt")
                        .with(user("teacher").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "/plagiarism"))
                .andExpect(flash().attributeExists("errorMessage"));

        List<com.group4.javagrader.entity.PlagiarismPair> refreshedPairs = plagiarismService.findPairsByReportId(firstReportId);
        assertThat(refreshedPairs)
                .filteredOn(pair -> pair.getId().equals(oldPairId))
                .singleElement()
                .matches(pair -> pair.getOverrideDecision() == null);
    }

    @Test
    void missingPlagiarismAssignmentRedirectsToSemesters() throws Exception {
        mockMvc.perform(get("/assignments/{assignmentId}/plagiarism", 999999L)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/semesters"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    private Long createAssignment() {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode("SP26-P4-" + System.nanoTime());
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Phase 4 View Lab");
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(30);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        return assignmentService.create(assignmentForm);
    }

    private void createProblemAndTestcase(Long assignmentId) {
        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Phase 4 Problem");
        problemForm.setMaxScore(BigDecimal.TEN);
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long problemId = problemService.create(problemForm);

        TestCaseForm testCaseForm = new TestCaseForm();
        testCaseForm.setProblemId(problemId);
        testCaseForm.setInputData("1 2");
        testCaseForm.setExpectedOutput("3");
        testCaseForm.setWeight(BigDecimal.ONE);
        testCaseService.create(testCaseForm);
    }

    private void uploadAndRunReport(Long assignmentId) throws Exception {
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase4-view.zip",
                        "s2211001/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s2211002/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s2211003/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"unique\"); } }"));

        plagiarismService.runReport(assignmentId, "teacher");
    }

    private MockMultipartFile archive(String fileName, String... nameContentPairs) throws IOException {
        return new MockMultipartFile(
                "archiveFile",
                fileName,
                "application/zip",
                zipOf(nameContentPairs));
    }

    private byte[] zipOf(String... nameContentPairs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                zipOutputStream.putNextEntry(new ZipEntry(nameContentPairs[i]));
                zipOutputStream.write(nameContentPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}
