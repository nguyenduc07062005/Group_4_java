package com.group4.javagrader.grading;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.PlagiarismService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.SubmissionService;
import com.group4.javagrader.service.TestCaseService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class PhaseFiveToSevenFlowIntegrationTest {

    private static final Pattern RESULT_LINK_PATTERN = Pattern.compile("/assignments/\\d+/grading\\?resultId=\\d+#result-detail");

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
    void batchProgressResultsAndReportsWorkForJavaCoreAssignments() throws Exception {
        Long assignmentId = createAssignment(GradingMode.JAVA_CORE, 100, 0, null);
        Long problemId = createProblem(assignmentId, "Warmup Sum", new BigDecimal("10.00"));
        createTestCase(problemId, "1 2\n", "3\n", new BigDecimal("1.00"));

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase5-core.zip",
                        "s2212001/Main.java", """
                                import java.util.*;
                                public class Main {
                                    public static void main(String[] args) {
                                        Scanner scanner = new Scanner(System.in);
                                        int a = scanner.nextInt();
                                        int b = scanner.nextInt();
                                        System.out.println(a + b);
                                    }
                                }
                                """,
                        "s2212002/Main.java", """
                                public class Main {
                                    public static void main(String[] args) {
                                        System.out.println("broken")
                                }
                                """));

        plagiarismService.runReport(assignmentId, "teacher");
        Batch batch = batchService.createBatch(assignmentId, "teacher");
        batchService.startBatch(assignmentId, batch.getId(), "teacher");

        Batch completedBatch = waitForBatchTerminal(batch.getId());
        assertThat(completedBatch.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_ERRORS);

        mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Grading Workspace")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212002")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DONE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FAILED_COMPILE")));

        MvcResult resultsPage = mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stored grading output")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212002")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gradebook CSV")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PDF Summary")))
                .andReturn();

        String resultDetailPath = extractFirstResultLink(resultsPage.getResponse().getContentAsString());
        mockMvc.perform(get(resultDetailPath)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Testcase Breakdown")));

        mockMvc.perform(get("/assignments/{assignmentId}/reports/gradebook.csv", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submitter,Status,Score,Max Score")));

        mockMvc.perform(get("/assignments/{assignmentId}/reports/detail.csv", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submitter,Problem,Test Case,Status,Earned Score")));

        MvcResult pdfResult = mockMvc.perform(get("/assignments/{assignmentId}/reports/summary.pdf", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertThat(new String(pdfBytes, 0, 5, StandardCharsets.UTF_8)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String extractedText = new PDFTextStripper().getText(document);
            assertThat(extractedText).contains("Java Code Grader Summary Report");
            assertThat(extractedText).contains("s2212001");
            assertThat(extractedText).contains("s2212002");
        }
    }

    @Test
    void javaCoreGradingAcceptsMarkdownWrappedInputAndTrailingPrintlnNewline() throws Exception {
        Long assignmentId = createAssignment(GradingMode.JAVA_CORE, 100, 0, null);
        Long problemId = createProblem(assignmentId, "Markdown Friendly Sum", new BigDecimal("10.00"));
        createTestCase(problemId, "`1 2`", "3", new BigDecimal("1.00"));

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase5-friendly.zip",
                        "s2212999/Main.java", """
                                import java.util.Scanner;
                                public class Main {
                                    public static void main(String[] args) {
                                        Scanner scanner = new Scanner(System.in);
                                        int a = scanner.nextInt();
                                        int b = scanner.nextInt();
                                        System.out.println(a + b);
                                    }
                                }
                                """));

        plagiarismService.runReport(assignmentId, "teacher");
        Batch batch = batchService.createBatch(assignmentId, "teacher");
        batchService.startBatch(assignmentId, batch.getId(), "teacher");

        Batch completedBatch = waitForBatchTerminal(batch.getId());
        assertThat(completedBatch.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_ERRORS);

        MvcResult resultsPage = mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212999")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10.00")))
                .andReturn();

        String resultDetailPath = extractFirstResultLink(resultsPage.getResponse().getContentAsString());
        mockMvc.perform(get(resultDetailPath)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PASSED")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Output matched expected result.")));
    }

    @Test
    void batchSnapshotStaysFrozenAfterCreateEvenIfEligiblePoolChangesLater() throws Exception {
        Long assignmentId = createAssignment(GradingMode.JAVA_CORE, 100, 0, null);
        Long problemId = createProblem(assignmentId, "Snapshot Sum", new BigDecimal("10.00"));
        createTestCase(problemId, "1 2\n", "3\n", new BigDecimal("1.00"));

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase5-snapshot-a.zip",
                        "s2212301/Main.java", """
                                import java.util.*;
                                public class Main {
                                    public static void main(String[] args) {
                                        Scanner scanner = new Scanner(System.in);
                                        int left = scanner.nextInt();
                                        int right = scanner.nextInt();
                                        System.out.println(left + right);
                                    }
                                }
                                """,
                        "s2212302/Main.java", """
                                import java.io.BufferedReader;
                                import java.io.InputStreamReader;
                                import java.util.StringTokenizer;
                                public class Main {
                                    public static void main(String[] args) throws Exception {
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                                        StringTokenizer tokenizer = new StringTokenizer(reader.readLine());
                                        int a = Integer.parseInt(tokenizer.nextToken());
                                        int b = Integer.parseInt(tokenizer.nextToken());
                                        System.out.println(a + b);
                                    }
                                }
                                """));

        plagiarismService.runReport(assignmentId, "teacher");
        Batch batch = batchService.createBatch(assignmentId, "teacher");

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase5-snapshot-b.zip",
                        "s2212303/Main.java", """
                                import java.io.BufferedInputStream;
                                public class Main {
                                    public static void main(String[] args) throws Exception {
                                        BufferedInputStream input = new BufferedInputStream(System.in);
                                        StringBuilder builder = new StringBuilder();
                                        int value;
                                        while ((value = input.read()) != -1) {
                                            builder.append((char) value);
                                        }
                                        String[] parts = builder.toString().trim().split("\\\\s+");
                                        System.out.println(Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]));
                                    }
                                }
                                """));

        plagiarismService.runReport(assignmentId, "teacher");
        batchService.startBatch(assignmentId, batch.getId(), "teacher");

        Batch completedBatch = waitForBatchTerminal(batch.getId());
        assertThat(completedBatch.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_ERRORS);

        mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("s2212301")))
                .andExpect(content().string(containsString("s2212302")))
                .andExpect(content().string(not(containsString("s2212303"))));

        mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("s2212301")))
                .andExpect(content().string(containsString("s2212302")))
                .andExpect(content().string(not(containsString("s2212303"))));
    }

    @Test
    void reportExportsStayHiddenAndRejectedUntilTheLatestBatchIsTerminal() throws Exception {
        Long assignmentId = createAssignment(GradingMode.JAVA_CORE, 100, 0, null);
        Long problemId = createProblem(assignmentId, "Export Gate Sum", new BigDecimal("10.00"));
        createTestCase(problemId, "1 2\n", "3\n", new BigDecimal("1.00"));

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase5-export-gate.zip",
                        "s2212401/Main.java", """
                                import java.util.Scanner;
                                public class Main {
                                    public static void main(String[] args) {
                                        Scanner scanner = new Scanner(System.in);
                                        int a = scanner.nextInt();
                                        int b = scanner.nextInt();
                                        System.out.println(a + b);
                                    }
                                }
                                """));

        plagiarismService.runReport(assignmentId, "teacher");
        batchService.createBatch(assignmentId, "teacher");

        mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(not(containsString("Download Gradebook"))))
                .andExpect(content().string(not(containsString("Download Detail CSV"))))
                .andExpect(content().string(not(containsString("Download PDF Summary"))));

        mockMvc.perform(get("/assignments/{assignmentId}/reports/gradebook.csv", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/assignments/{assignmentId}/reports/detail.csv", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/assignments/{assignmentId}/reports/summary.pdf", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isConflict());
    }

    @Test
    void oopAssignmentsProduceRuleViolationsAndWeightedScores() throws Exception {
        String oopRulesJson = """
                {
                  "rules": [
                    { "type": "minimum_class_count", "value": 2, "label": "At least two classes" },
                    { "type": "required_keyword", "value": "extends", "label": "Uses inheritance" },
                    { "type": "required_keyword", "value": "private", "label": "Uses encapsulation" }
                  ]
                }
                """;

        Long assignmentId = createAssignment(GradingMode.OOP, 60, 40, oopRulesJson);
        Long problemId = createProblem(assignmentId, "OOP Runtime", new BigDecimal("10.00"));
        createTestCase(problemId, "", "Alice\n", new BigDecimal("1.00"));

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase6-oop.zip",
                        "s2212101/src/main/java/demo/BasePerson.java", """
                                package demo;
                                public class BasePerson {
                                    protected final String name;
                                    public BasePerson(String name) {
                                        this.name = name;
                                    }
                                }
                                """,
                        "s2212101/src/main/java/demo/Student.java", """
                                package demo;
                                public class Student extends BasePerson {
                                    private final String studentName;
                                    public Student(String studentName) {
                                        super(studentName);
                                        this.studentName = studentName;
                                    }
                                    public String getStudentName() {
                                        return studentName;
                                    }
                                }
                                """,
                        "s2212101/src/main/java/demo/Main.java", """
                                package demo;
                                public class Main {
                                    public static void main(String[] args) {
                                        Student student = new Student("Alice");
                                        System.out.println(student.getStudentName());
                                    }
                                }
                                """,
                        "s2212102/src/main/java/demo/Student.java", """
                                package demo;
                                public class Student {
                                    public String name = "Alice";
                                }
                                """,
                        "s2212102/src/main/java/demo/Main.java", """
                                package demo;
                                public class Main {
                                    public static void main(String[] args) {
                                        Student student = new Student();
                                        System.out.println(student.name);
                                    }
                                }
                                """));

        plagiarismService.runReport(assignmentId, "teacher");
        Batch batch = batchService.createBatch(assignmentId, "teacher");
        batchService.startBatch(assignmentId, batch.getId(), "teacher");

        Batch completedBatch = waitForBatchTerminal(batch.getId());
        assertThat(completedBatch.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_ERRORS);

        MvcResult resultsPage = mockMvc.perform(get("/assignments/{assignmentId}/grading", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212101")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("s2212102")))
                .andReturn();

        String resultDetailPath = extractFirstResultLink(resultsPage.getResponse().getContentAsString());
        mockMvc.perform(get(resultDetailPath)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("grading/workspace"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OOP Rule Evaluation")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Uses inheritance")));
    }

    private Long createAssignment(GradingMode gradingMode, int logicWeight, int oopWeight, String oopRulesJson) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode("SP26-P5-" + System.nanoTime());
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Phase 5-7 Assignment " + gradingMode.name());
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(gradingMode);
        assignmentForm.setPlagiarismThreshold(95);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        assignmentForm.setLogicWeight(logicWeight);
        assignmentForm.setOopWeight(oopWeight);
        if (oopRulesJson != null) {
            assignmentForm.setOopRuleConfig(new MockMultipartFile(
                    "oopRuleConfig",
                    "rules.json",
                    "application/json",
                    oopRulesJson.getBytes(StandardCharsets.UTF_8)));
        }
        return assignmentService.create(assignmentForm);
    }

    private Long createProblem(Long assignmentId, String title, BigDecimal maxScore) {
        ProblemForm form = new ProblemForm();
        form.setAssignmentId(assignmentId);
        form.setTitle(title);
        form.setMaxScore(maxScore);
        form.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        form.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        return problemService.create(form);
    }

    private Long createTestCase(Long problemId, String inputData, String expectedOutput, BigDecimal weight) {
        TestCaseForm form = new TestCaseForm();
        form.setProblemId(problemId);
        form.setInputData(inputData);
        form.setExpectedOutput(expectedOutput);
        form.setWeight(weight);
        return testCaseService.create(form);
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

    private String extractFirstResultLink(String html) {
        Matcher matcher = RESULT_LINK_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }
}
