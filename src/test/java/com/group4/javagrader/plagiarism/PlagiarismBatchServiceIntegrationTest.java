package com.group4.javagrader.plagiarism;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.BatchPrecheckView;
import com.group4.javagrader.dto.SubmissionUploadSummary;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.PlagiarismPair;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.PlagiarismService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PlagiarismBatchServiceIntegrationTest {

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private PlagiarismService plagiarismService;

    @Autowired
    private BatchService batchService;

    @Test
    void runReportFlagsSimilarValidatedSubmissionsAndOverrideReleasesBatchPrecheck() throws Exception {
        Long assignmentId = createAssignment("P4-SIM", 50);

        SubmissionUploadSummary uploadSummary = submissionService.uploadArchive(
                assignmentId,
                archive("phase4-similar.zip",
                        "s2210001/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s2210002/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s2210003/Main.java", "public class Main { public static void main(String[] args) { String[] words = {\"alpha\", \"beta\"}; for (String word : words) { System.out.print(word.length()); } } }"));

        assertThat(uploadSummary.getValidatedCount()).isEqualTo(3);

        PlagiarismReport report = plagiarismService.runReport(assignmentId, "teacher");
        List<PlagiarismPair> pairs = plagiarismService.findPairsByReportId(report.getId());

        assertThat(report.getTotalSubmissions()).isEqualTo(3);
        assertThat(report.getFlaggedPairCount()).isEqualTo(1);
        assertThat(report.getBlockedSubmissionCount()).isEqualTo(2);
        assertThat(pairs).hasSize(3);

        PlagiarismPair blockedPair = pairs.stream()
                .filter(PlagiarismPair::isEffectivelyBlocked)
                .findFirst()
                .orElseThrow();

        BatchPrecheckView precheckBeforeOverride = batchService.buildPrecheck(assignmentId);
        assertThat(precheckBeforeOverride.getGradeableEntries()).hasSize(1);
        assertThat(precheckBeforeOverride.getExcludedEntries()).hasSize(2);

        plagiarismService.overridePair(
                assignmentId,
                blockedPair.getId(),
                "ALLOW",
                "Manual review accepted shared starter code.",
                "teacher");

        PlagiarismReport refreshedReport = plagiarismService.findLatestReport(assignmentId).orElseThrow();
        assertThat(refreshedReport.getFlaggedPairCount()).isEqualTo(0);
        assertThat(refreshedReport.getBlockedSubmissionCount()).isEqualTo(0);

        BatchPrecheckView precheckAfterOverride = batchService.buildPrecheck(assignmentId);
        assertThat(precheckAfterOverride.getGradeableEntries()).hasSize(3);
        assertThat(precheckAfterOverride.getExcludedEntries()).isEmpty();
    }

    @Test
    void createAndStartBatchUsesBoundedQueueAndReachesTerminalStateAfterReadyForGrading() throws Exception {
        Long assignmentId = createAssignment("P4-BATCH", 40);

        SubmissionUploadSummary uploadSummary = submissionService.uploadArchive(
                assignmentId,
                archive("phase4-batch.zip",
                        "s2210101/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"A\"); } }",
                        "s2210102/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"B\"); } }"));

        assertThat(uploadSummary.getValidatedCount()).isEqualTo(2);

        plagiarismService.runReport(assignmentId, "teacher");

        Batch batch = batchService.createBatch(assignmentId, "teacher");
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PRECHECKED);
        assertThat(batch.getGradeableSubmissionCount()).isEqualTo(2);

        Batch startedBatch = batchService.startBatch(assignmentId, batch.getId(), "teacher");
        assertThat(startedBatch.getStatus()).isIn(
                BatchStatus.QUEUED,
                BatchStatus.READY_FOR_GRADING,
                BatchStatus.RUNNING,
                BatchStatus.COMPLETED,
                BatchStatus.COMPLETED_WITH_ERRORS);

        Batch completedBatch = waitForTerminal(batch.getId());
        assertThat(completedBatch.getStatus()).isIn(BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(completedBatch.getStartedBy()).isEqualTo("teacher");
        assertThat(completedBatch.getReadyAt()).isNotNull();
        assertThat(completedBatch.getCompletedAt()).isNotNull();
    }

    @Test
    void overrideRejectsPairsFromOlderReportsAfterAssignmentRerunsPlagiarism() throws Exception {
        Long assignmentId = createAssignment("P4-HISTORY", 50);

        SubmissionUploadSummary uploadSummary = submissionService.uploadArchive(
                assignmentId,
                archive("phase4-history.zip",
                        "s2210201/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s2210202/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s2210203/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"unique\"); } }"));

        assertThat(uploadSummary.getValidatedCount()).isEqualTo(3);

        PlagiarismReport firstReport = plagiarismService.runReport(assignmentId, "teacher");
        PlagiarismPair firstBlockedPair = plagiarismService.findPairsByReportId(firstReport.getId()).stream()
                .filter(PlagiarismPair::isEffectivelyBlocked)
                .findFirst()
                .orElseThrow();

        PlagiarismReport secondReport = plagiarismService.runReport(assignmentId, "teacher");
        assertThat(plagiarismService.findLatestReport(assignmentId))
                .get()
                .extracting(PlagiarismReport::getId)
                .isEqualTo(secondReport.getId());

        assertThatThrownBy(() -> plagiarismService.overridePair(
                assignmentId,
                firstBlockedPair.getId(),
                "ALLOW",
                "Trying to modify an old report.",
                "teacher"))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("latest report");

        assertThat(plagiarismService.findPairsByReportId(firstReport.getId()))
                .filteredOn(pair -> pair.getId().equals(firstBlockedPair.getId()))
                .singleElement()
                .matches(pair -> pair.getOverrideDecision() == null);
    }

    @Test
    void buildDashboardRejectsMissingAssignmentAsResourceNotFound() {
        assertThatThrownBy(() -> plagiarismService.buildDashboard(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void overrideRejectsInvalidDecisionAsInputValidation() throws Exception {
        Long assignmentId = createAssignment("P4-BAD-DECISION", 50);

        assertThatThrownBy(() -> plagiarismService.overridePair(
                assignmentId,
                123L,
                "MAYBE",
                null,
                "teacher"))
                .isInstanceOf(InputValidationException.class)
                .hasMessage("Override decision must be ALLOW or BLOCK.");
    }

    @Test
    void overrideRejectsMissingReportAsWorkflowState() throws Exception {
        Long assignmentId = createAssignment("P4-NO-REPORT", 50);

        assertThatThrownBy(() -> plagiarismService.overridePair(
                assignmentId,
                123L,
                "ALLOW",
                null,
                "teacher"))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("No plagiarism report is available for this assignment.");
    }

    @Test
    void overrideRejectsPairFromAnotherAssignmentAsOwnershipViolation() throws Exception {
        Long firstAssignmentId = createAssignment("P4-OWNER-A", 50);
        uploadSimilarSubmissionsAndRunReport(firstAssignmentId);
        Long secondAssignmentId = createAssignment("P4-OWNER-B", 50);
        uploadSimilarSubmissionsAndRunReport(secondAssignmentId);

        Long secondReportId = plagiarismService.findLatestReport(secondAssignmentId).orElseThrow().getId();
        Long foreignPairId = plagiarismService.findPairsByReportId(secondReportId).stream()
                .filter(PlagiarismPair::isEffectivelyBlocked)
                .findFirst()
                .orElseThrow()
                .getId();

        assertThatThrownBy(() -> plagiarismService.overridePair(
                firstAssignmentId,
                foreignPairId,
                "ALLOW",
                "Cross assignment pair.",
                "teacher"))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Plagiarism pair does not belong to this assignment.");
    }

    @Test
    void uploadRejectsChangesWhileBatchSnapshotIsActive() throws Exception {
        Long assignmentId = createAssignment("P4-SNAPSHOT", 40);

        SubmissionUploadSummary uploadSummary = submissionService.uploadArchive(
                assignmentId,
                archive("phase4-snapshot.zip",
                        "s2210301/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"A\"); } }",
                        "s2210302/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"B\"); } }"));

        assertThat(uploadSummary.getValidatedCount()).isEqualTo(2);
        plagiarismService.runReport(assignmentId, "teacher");
        Batch batch = batchService.createBatch(assignmentId, "teacher");
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PRECHECKED);

        assertThatThrownBy(() -> submissionService.uploadArchive(
                assignmentId,
                archive("after-snapshot.zip",
                        "s2210301/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"changed\"); } }")))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("batch snapshot");

        assertThat(submissionService.findByAssignmentId(assignmentId))
                .filteredOn(submission -> "s2210301".equals(submission.getSubmitterName()))
                .singleElement()
                .extracting(Submission::getArchiveFileName)
                .isEqualTo("phase4-snapshot.zip");
    }

    private Long createAssignment(String codePrefix, int threshold) {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode(codePrefix + "-" + System.nanoTime());
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Phase 4 Assignment " + codePrefix);
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(threshold);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        return assignmentService.create(assignmentForm);
    }

    private void uploadSimilarSubmissionsAndRunReport(Long assignmentId) throws Exception {
        submissionService.uploadArchive(
                assignmentId,
                archive(
                        "phase4-owner-" + assignmentId + ".zip",
                        "s" + assignmentId + "01/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }",
                        "s" + assignmentId + "02/Main.java", "public class Main { public static void main(String[] args) { int sum = 1 + 2; System.out.println(sum); } }"));
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

    private Batch waitForTerminal(Long batchId) throws InterruptedException {
        long timeoutAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < timeoutAt) {
            Batch batch = batchService.findById(batchId).orElseThrow();
            if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS) {
                return batch;
            }
            Thread.sleep(100);
        }
        return batchService.findById(batchId).orElseThrow();
    }
}
