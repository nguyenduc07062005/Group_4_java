package com.group4.javagrader.problem;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.ResultDetailView;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.PlagiarismService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.ResultService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.SubmissionService;
import com.group4.javagrader.service.TestCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProblemHistoryIntegrationTest {

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

    @Autowired
    private ResultService resultService;

    @Test
    void creatingFirstVisibleProblemAfterGradingKeepsHistoricalRuntimeProblemStable() throws Exception {
        Long assignmentId = createAssignment();
        Problem runtimeProblem = problemService.findPrimaryProblemByAssignmentId(assignmentId).orElseThrow();
        createTestCase(runtimeProblem.getId());
        uploadPassingSubmission(assignmentId);

        plagiarismService.runReport(assignmentId, "teacher");
        Batch batch = batchService.createBatch(assignmentId, "teacher");
        batchService.startBatch(assignmentId, batch.getId(), "teacher");
        waitForBatchTerminal(batch.getId());

        ResultDetailView beforeCreate = resultService.buildDetailsForLatestBatch(assignmentId).orElseThrow().get(0);
        assertThat(beforeCreate.getProblemResults()).hasSize(1);
        assertThat(beforeCreate.getProblemResults().get(0).getTitle()).isEqualTo("Assignment Runtime");

        Long visibleProblemId = createVisibleProblem(assignmentId);

        assertThat(visibleProblemId).isNotEqualTo(runtimeProblem.getId());
        assertThat(problemService.findByAssignmentId(assignmentId))
                .extracting(Problem::getId)
                .containsExactly(visibleProblemId);

        ResultDetailView afterCreate = resultService.buildDetailsForLatestBatch(assignmentId).orElseThrow().get(0);
        assertThat(afterCreate.getProblemResults()).hasSize(1);
        assertThat(afterCreate.getProblemResults().get(0).getTitle()).isEqualTo("Assignment Runtime");
    }

    private Long createAssignment() {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode("PH26-" + System.nanoTime());
        semesterForm.setName("Problem History");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Runtime History Lab");
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(95);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        return assignmentService.create(assignmentForm);
    }

    private void createTestCase(Long problemId) {
        TestCaseForm form = new TestCaseForm();
        form.setProblemId(problemId);
        form.setInputData("1 2\n");
        form.setExpectedOutput("3\n");
        form.setWeight(BigDecimal.ONE);
        testCaseService.create(form);
    }

    private Long createVisibleProblem(Long assignmentId) {
        ProblemForm form = new ProblemForm();
        form.setAssignmentId(assignmentId);
        form.setTitle("Visible Follow-up Question");
        form.setMaxScore(BigDecimal.TEN);
        form.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        form.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        return problemService.create(form);
    }

    private void uploadPassingSubmission(Long assignmentId) throws IOException {
        submissionService.uploadArchive(
                assignmentId,
                new MockMultipartFile(
                        "archiveFile",
                        "history-lab.zip",
                        "application/zip",
                        zipOf(
                                "s2212998/Main.java", """
                                        import java.util.Scanner;
                                        public class Main {
                                            public static void main(String[] args) {
                                                Scanner scanner = new Scanner(System.in);
                                                int a = scanner.nextInt();
                                                int b = scanner.nextInt();
                                                System.out.println(a + b);
                                            }
                                        }
                                        """)));
    }

    private byte[] zipOf(String fileName, String content) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry(fileName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }

    private Batch waitForBatchTerminal(Long batchId) throws InterruptedException {
        long timeoutAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < timeoutAt) {
            Batch batch = batchService.findById(batchId).orElseThrow();
            if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS) {
                return batch;
            }
            Thread.sleep(150);
        }
        return batchService.findById(batchId).orElseThrow();
    }
}
