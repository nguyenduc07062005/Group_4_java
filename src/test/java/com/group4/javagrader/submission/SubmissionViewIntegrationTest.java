package com.group4.javagrader.submission;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.SubmissionService;
import com.group4.javagrader.service.TestCaseService;
import com.group4.javagrader.storage.SubmissionStorageService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class SubmissionViewIntegrationTest {

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
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionStorageService submissionStorageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void assignmentDetailShowsUploadSubmissionsAction() throws Exception {
        Long assignmentId = createAssignment(true);

        mockMvc.perform(get("/assignments/{assignmentId}", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Submission Intake")))
                .andExpect(content().string(containsString("Ready for intake")));
    }

    @Test
    void assignmentDetailHidesUploadUntilTestcaseCoverageIsReady() throws Exception {
        Long assignmentId = createAssignment(false);

        mockMvc.perform(get("/assignments/{assignmentId}", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/detail"))
                .andExpect(content().string(containsString("Still needs testcase coverage")))
                .andExpect(content().string(containsString("Submission Intake")))
                .andExpect(content().string(not(containsString("Ready for intake"))));
    }

    @Test
    void submissionUploadPageRendersInstructionsAndStatusSections() throws Exception {
        Long assignmentId = createAssignment(true);

        mockMvc.perform(get("/assignments/{assignmentId}/submissions/upload", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("submission/upload"))
                .andExpect(content().string(containsString("Submission Intake")))
                .andExpect(content().string(containsString("One ZIP, many submissions")))
                .andExpect(content().string(containsString("Archive Layout")))
                .andExpect(content().string(containsString("Expected ZIP structure")))
                .andExpect(content().string(containsString("Current intake status")))
                .andExpect(content().string(not(containsString("Accepted extensions:"))))
                .andExpect(content().string(not(containsString("Only VALIDATED moves forward"))));
    }

    @Test
    void submissionUploadPageShowsDirectLinkToPlagiarismWhenValidatedSubmissionsExist() throws Exception {
        Long assignmentId = createAssignment(true);
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "submission-flow.zip",
                        "s2213001/Main.java", "public class Main { public static void main(String[] args) { System.out.println(3); } }",
                        "s2213002/Main.java", "public class Main { public static void main(String[] args) { System.out.println(3); } }"));

        mockMvc.perform(get("/assignments/{assignmentId}/submissions/upload", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("submission/upload"))
                .andExpect(content().string(containsString("Open Plagiarism Review")));
    }

    @Test
    void submissionUploadPageShowsFixStructureForRejectedSubmissionThatCanBeRepairedFromPaths() throws Exception {
        Long assignmentId = createAssignment(true);
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "bad-structure.zip",
                        "s2213001/src/Main.java", "public class Main { public static void main(String[] args) { System.out.println(3); } }"));

        mockMvc.perform(get("/assignments/{assignmentId}/submissions/upload", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("submission/upload"))
                .andExpect(content().string(containsString("Fix Structure")));
    }

    @Test
    void submissionUploadPageHidesFixStructureForRejectedSubmissionThatNeedsSourceChanges() throws Exception {
        Long assignmentId = createAssignment(true);
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "bad-package.zip",
                        "s2213001/Main.java", "package bad; public class Main {}"));

        mockMvc.perform(get("/assignments/{assignmentId}/submissions/upload", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("submission/upload"))
                .andExpect(content().string(not(containsString("Fix Structure"))));
    }

    @Test
    void fixStructurePageRejectsSubmissionThatCannotBeRepairedFromPathsAlone() throws Exception {
        Long assignmentId = createAssignment(true);
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "bad-package.zip",
                        "s2213001/Main.java", "package bad; public class Main {}"));
        Submission submission = submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId).get(0);

        mockMvc.perform(get("/assignments/{assignmentId}/submissions/{submissionId}/fix-structure", assignmentId, submission.getId())
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "/submissions/upload"))
                .andExpect(flash().attribute("errorMessage", containsString("cannot be fixed from the web editor")));
    }

    @Test
    void fixStructurePageOpensForRejectedSubmissionThatCanBeRepairedFromPaths() throws Exception {
        Long assignmentId = createAssignment(true);
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "bad-structure.zip",
                        "s2213001/src/Main.java", "public class Main { public static void main(String[] args) { System.out.println(3); } }"));
        Submission submission = submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId).get(0);

        mockMvc.perform(get("/assignments/{assignmentId}/submissions/{submissionId}/fix-structure", assignmentId, submission.getId())
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("submission/fix-structure"))
                .andExpect(content().string(containsString("Fix Submission Structure")));
    }

    @Test
    void rejectedArchiveUploadShowsWarningFlashInsteadOfSuccess() throws Exception {
        Long assignmentId = createAssignment(true);

        mockMvc.perform(multipart("/assignments/{assignmentId}/submissions/upload", assignmentId)
                        .file(archive(
                                "bad-submission.zip",
                                "s2213001/Main.java",
                                "package bad; public class Main {}"))
                        .with(csrf())
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "/submissions/upload#stored-submissions"))
                .andExpect(flash().attribute("warningMessage", containsString("1 rejected")))
                .andExpect(flash().attribute("uploadAnchor", "stored-submissions"))
                .andExpect(flash().attributeCount(3));
    }

    @Test
    void parseRejectedArchiveReturnsToUploadAnchorWithError() throws Exception {
        Long assignmentId = createAssignment(true);

        mockMvc.perform(multipart("/assignments/{assignmentId}/submissions/upload", assignmentId)
                        .file(archive(
                                "bad-layout.zip",
                                "Main.java",
                                "public class Main {}"))
                        .with(csrf())
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/" + assignmentId + "/submissions/upload#intake-upload"))
                .andExpect(flash().attribute("errorMessage", containsString("top-level student folder")))
                .andExpect(flash().attribute("uploadAnchor", "intake-upload"));
    }

    @Test
    void repeatedUploadForSameSubmitterReplacesCurrentSubmissionState() throws Exception {
        Long assignmentId = createAssignment(true);

        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "first-submission.zip",
                        "s2213001/Main.java", "public class Main { public static void main(String[] args) { System.out.println(1); } }",
                        "s2213001/OldHelper.java", "class OldHelper {}"));
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "second-submission.zip",
                        "s2213001/Main.java", "public class Main { public static void main(String[] args) { System.out.println(2); } }"));

        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId);

        assertThat(submissions).hasSize(1);
        Submission submission = submissions.get(0);
        assertThat(submission.getSubmitterName()).isEqualTo("s2213001");
        assertThat(submission.getArchiveFileName()).isEqualTo("second-submission.zip");
        assertThat(submission.getFileCount()).isEqualTo(1);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.VALIDATED);

        assertThat(submissionStorageService.loadSubmissionFiles(submission.getStoragePath()))
                .extracting(SubmissionFile::relativePath)
                .containsExactly("Main.java");
    }

    @Test
    void missingSubmissionAssignmentRedirectsToSemesters() throws Exception {
        mockMvc.perform(get("/assignments/{assignmentId}/submissions/upload", 999999L)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/semesters"))
                .andExpect(flash().attribute("errorMessage", "Assignment not found."));
    }

    private Long createAssignment(boolean withReadyTestcaseSetup) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode("SP26-P3-" + System.nanoTime());
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Submission Upload Lab");
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(35);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.TRIM_ALL);
        Long assignmentId = assignmentService.create(assignmentForm);

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Submission Problem");
        problemForm.setMaxScore(BigDecimal.TEN);
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long problemId = problemService.create(problemForm);

        if (withReadyTestcaseSetup) {
            TestCaseForm testCaseForm = new TestCaseForm();
            testCaseForm.setProblemId(problemId);
            testCaseForm.setInputData("1 2");
            testCaseForm.setExpectedOutput("3");
            testCaseForm.setWeight(BigDecimal.ONE);
            testCaseService.create(testCaseForm);
        }

        return assignmentId;
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
