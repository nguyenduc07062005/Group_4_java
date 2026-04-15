package com.group4.javagrader.submission;

import com.group4.javagrader.dto.SubmissionUploadGuide;
import com.group4.javagrader.dto.SubmissionUploadSummary;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.GradingResultStatus;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.validation.AllowedExtensionValidationStep;
import com.group4.javagrader.grading.validation.MaxDepthValidationStep;
import com.group4.javagrader.grading.validation.MaxFilesValidationStep;
import com.group4.javagrader.grading.validation.ModeConstraintValidationStep;
import com.group4.javagrader.grading.validation.PackageConventionValidationStep;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.service.impl.SubmissionServiceImpl;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SubmissionServiceUploadTest {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "java", "txt", "md", "xml", "properties", "json", "yml", "yaml", "csv", "gradle", "kts");

    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
    private final BatchRepository batchRepository = mock(BatchRepository.class);
    private final GradingResultRepository gradingResultRepository = mock(GradingResultRepository.class);
    private final SubmissionStorageService submissionStorageService = mock(SubmissionStorageService.class);
    private final SubmissionServiceImpl service = new SubmissionServiceImpl(
            submissionRepository,
            assignmentRepository,
            batchRepository,
            gradingResultRepository,
            submissionStorageService,
            List.of(
                    new MaxFilesValidationStep(50),
                    new MaxDepthValidationStep(6),
                    new AllowedExtensionValidationStep(ALLOWED_EXTENSIONS),
                    new PackageConventionValidationStep(),
                    new ModeConstraintValidationStep()
            ));

    @Test
    void uploadGuideReflectsCurrentValidationLimits() {
        SubmissionUploadGuide guide = service.getUploadGuide();

        assertThat(guide.getMaxArchiveSizeMb()).isEqualTo(20L);
        assertThat(guide.getMaxFilesPerSubmission()).isEqualTo(50);
        assertThat(guide.getMaxFolderDepth()).isEqualTo(6);
        assertThat(guide.getAllowedExtensions()).containsExactly(
                "csv", "gradle", "java", "json", "kts", "md", "properties", "txt", "xml", "yaml", "yml");
    }

    @Test
    void uploadArchiveValidatesJavaCoreSubmissionAndStoresFiles() throws Exception {
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment(55L, GradingMode.JAVA_CORE)));
        when(submissionStorageService.storeSubmissionFiles(any(), anyLong(), eq("s2210001"), anyList()))
                .thenReturn("stored/55/s2210001");
        stubGeneratedSubmissionIds();

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210001/Main.java", "public class Main { public static void main(String[] args) { System.out.println(\"ok\"); } }",
                        "s2210001/Helper.java", "class Helper {}"
                ));

        SubmissionUploadSummary summary = service.uploadArchive(55L, archiveFile);

        assertThat(summary.getTotalSubmissions()).isEqualTo(1);
        assertThat(summary.getValidatedCount()).isEqualTo(1);
        assertThat(summary.getRejectedCount()).isEqualTo(0);

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository, atLeastOnce()).save(submissionCaptor.capture());
        Submission savedSubmission = submissionCaptor.getAllValues().get(submissionCaptor.getAllValues().size() - 1);

        assertThat(savedSubmission.getSubmitterName()).isEqualTo("s2210001");
        assertThat(savedSubmission.getArchiveFileName()).isEqualTo("submissions.zip");
        assertThat(savedSubmission.getStatus()).isEqualTo(SubmissionStatus.VALIDATED);
        assertThat(savedSubmission.getStoragePath()).isEqualTo("stored/55/s2210001");
        assertThat(savedSubmission.getFileCount()).isEqualTo(2);
        assertThat(savedSubmission.getValidationCode()).isNull();

        verify(submissionStorageService).storeSubmissionFiles(any(), anyLong(), eq("s2210001"), anyList());
    }

    @Test
    void uploadArchiveValidatesOopSubmissionWhenPackageMatchesSourceTree() throws Exception {
        when(assignmentRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(assignment(77L, GradingMode.OOP)));
        when(submissionStorageService.storeSubmissionFiles(any(), anyLong(), eq("s2210003"), anyList()))
                .thenReturn("stored/77/s2210003");
        stubGeneratedSubmissionIds();

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210003/src/com/example/App.java", "package com.example; public class App {}",
                        "s2210003/src/com/example/DomainModel.java", "package com.example; class DomainModel {}"
                ));

        SubmissionUploadSummary summary = service.uploadArchive(77L, archiveFile);

        assertThat(summary.getTotalSubmissions()).isEqualTo(1);
        assertThat(summary.getValidatedCount()).isEqualTo(1);
        assertThat(summary.getRejectedCount()).isEqualTo(0);

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository, atLeastOnce()).save(submissionCaptor.capture());
        Submission savedSubmission = submissionCaptor.getAllValues().get(submissionCaptor.getAllValues().size() - 1);

        assertThat(savedSubmission.getStatus()).isEqualTo(SubmissionStatus.VALIDATED);
        assertThat(savedSubmission.getSubmitterName()).isEqualTo("s2210003");
        assertThat(savedSubmission.getStoragePath()).isEqualTo("stored/77/s2210003");
    }
    @Test
    void uploadArchiveMarksJavaCoreSubmissionRejectedWhenPackageDeclarationExists() throws Exception {
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment(55L, GradingMode.JAVA_CORE)));
        stubGeneratedSubmissionIds();

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210002/Main.java", "package bad; public class Main {}"
                ));

        SubmissionUploadSummary summary = service.uploadArchive(55L, archiveFile);

        assertThat(summary.getTotalSubmissions()).isEqualTo(1);
        assertThat(summary.getValidatedCount()).isEqualTo(0);
        assertThat(summary.getRejectedCount()).isEqualTo(1);

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository, atLeastOnce()).save(submissionCaptor.capture());
        Submission savedSubmission = submissionCaptor.getAllValues().get(submissionCaptor.getAllValues().size() - 1);

        assertThat(savedSubmission.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(savedSubmission.getValidationCode()).isEqualTo("JAVA_CORE_PACKAGE_NOT_ALLOWED");
        assertThat(savedSubmission.getValidationMessage()).contains("package declaration");
        assertThat(savedSubmission.getStoragePath()).isNull();

        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveRejectsUnsafeZipEntryBeforePersistence() throws Exception {
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment(55L, GradingMode.JAVA_CORE)));

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "../evil/Main.java", "public class Main {}"
                ));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining("unsafe path");

        verify(submissionRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(submissionStorageService);
        verifyNoMoreInteractions(submissionRepository);
    }

    @Test
    void uploadArchiveRejectsDuplicateSubmitterFoldersThatDifferOnlyByCase() throws Exception {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210001/Main.java", "public class Main {}",
                        "S2210001/Helper.java", "class Helper {}"
                ));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining("duplicate student folders");

        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(submissionRepository);
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveRejectsDuplicateNormalizedPathsWithinSameSubmission() throws Exception {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210001/src/Main.java", "public class Main {}",
                        "s2210001/src/./Main.java", "class Shadow {}"
                ));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining("duplicate file paths");

        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(submissionRepository);
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveRejectsExpandedContentBeyondSafetyLimit() throws Exception {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOfRepeatedEntry("s2210001/Huge.java", (byte) 'A', 51 * 1024 * 1024));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining("50 MB safety limit");

        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(submissionRepository);
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveThrowsResourceNotFoundWhenAssignmentDoesNotExist() throws Exception {
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.empty());

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210001/Main.java",
                        "public class Main { public static void main(String[] args) { System.out.println(\"ok\"); } }"));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assignment not found");

        verify(assignmentRepository).findByIdForUpdate(55L);
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveLocksAssignmentBeforeResolvingSubmitterRows() throws Exception {
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment(55L, GradingMode.JAVA_CORE)));
        when(submissionStorageService.storeSubmissionFiles(any(), anyLong(), eq("s2210001"), anyList()))
                .thenReturn("stored/55/s2210001");
        stubGeneratedSubmissionIds();

        service.uploadArchive(55L, new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210001/Main.java",
                        "public class Main { public static void main(String[] args) { System.out.println(\"ok\"); } }")));

        verify(assignmentRepository).findByIdForUpdate(55L);
        verify(assignmentRepository, org.mockito.Mockito.never()).findById(55L);
    }

    @Test
    void uploadArchiveRejectsReuploadWhenSubmissionHasGradingSnapshot() throws Exception {
        Assignment assignment = assignment(55L, GradingMode.JAVA_CORE);
        Submission existingSubmission = submission(100L, assignment, "s2210001", "old.zip", SubmissionStatus.VALIDATED, "stored/old");
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findByAssignmentIdAndSubmitterNameIgnoreCaseOrderByIdDesc(55L, "s2210001"))
                .thenReturn(List.of(existingSubmission));
        when(gradingResultRepository.existsBySubmissionId(100L)).thenReturn(true);

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "new-submission.zip",
                "application/zip",
                zipOf(
                        "s2210001/Main.java",
                        "public class Main { public static void main(String[] args) { System.out.println(\"new\"); } }"));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("already part of a grading snapshot");

        verify(submissionRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveChecksAllNonTerminalBatchesBeforeMutatingFrozenSubmitter() throws Exception {
        Assignment assignment = assignment(55L, GradingMode.JAVA_CORE);
        Batch olderRunningBatch = batch(201L, assignment, BatchStatus.RUNNING);
        Batch newerPrecheckedBatch = batch(202L, assignment, BatchStatus.PRECHECKED);
        Submission frozenSubmission = submission(100L, assignment, "s2210001", "old.zip", SubmissionStatus.VALIDATED, "stored/old");
        GradingResult frozenResult = gradingResult(frozenSubmission);

        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment));
        when(batchRepository.findByAssignmentIdAndStatusNotInOrderByIdDesc(
                eq(55L),
                eq(List.of(BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_ERRORS))))
                .thenReturn(List.of(newerPrecheckedBatch, olderRunningBatch));
        when(gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(202L)).thenReturn(List.of());
        when(gradingResultRepository.findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(201L)).thenReturn(List.of(frozenResult));

        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "new-submission.zip",
                "application/zip",
                zipOf(
                        "s2210001/Main.java",
                        "public class Main { public static void main(String[] args) { System.out.println(\"new\"); } }"));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessageContaining("batch snapshot #201");

        verify(submissionRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void uploadArchiveRejectsTooManyZipEntriesBeforePersistence() throws Exception {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "too-many-files.zip",
                "application/zip",
                zipWithEntryCount(10_001));

        assertThatThrownBy(() -> service.uploadArchive(55L, archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining("too many ZIP entries");

        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(submissionRepository);
        verifyNoInteractions(submissionStorageService);
    }

    @Test
    void rejectedReuploadDeletesPreviousStoredSnapshot() throws Exception {
        Assignment assignment = assignment(55L, GradingMode.JAVA_CORE);
        Submission existingSubmission = submission(100L, assignment, "s2210001", "old.zip", SubmissionStatus.VALIDATED, "stored/old");
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findByAssignmentIdAndSubmitterNameIgnoreCaseOrderByIdDesc(55L, "s2210001"))
                .thenReturn(List.of(existingSubmission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionUploadSummary summary = service.uploadArchive(
                55L,
                new MockMultipartFile(
                        "archiveFile",
                        "bad-reupload.zip",
                        "application/zip",
                        zipOf(
                                "s2210001/Main.java",
                                "package bad; public class Main {}")));

        assertThat(summary.getValidatedCount()).isEqualTo(0);
        assertThat(summary.getRejectedCount()).isEqualTo(1);
        verify(submissionStorageService).deleteSubmissionFiles("stored/old");
        verify(submissionStorageService, org.mockito.Mockito.never()).storeSubmissionFiles(any(), anyLong(), any(), anyList());
    }

    @Test
    void newStoredSubmissionIsCleanedUpWhenTransactionRollsBack() throws Exception {
        when(assignmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(assignment(55L, GradingMode.JAVA_CORE)));
        when(submissionStorageService.storeSubmissionFiles(any(), anyLong(), eq("s2210001"), anyList()))
                .thenReturn("stored/55/s2210001");
        stubGeneratedSubmissionIds();

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.uploadArchive(
                    55L,
                    new MockMultipartFile(
                            "archiveFile",
                            "submissions.zip",
                            "application/zip",
                            zipOf(
                                    "s2210001/Main.java",
                                    "public class Main { public static void main(String[] args) { System.out.println(\"ok\"); } }")));

            verify(submissionStorageService, org.mockito.Mockito.never()).deleteSubmissionFiles("stored/55/s2210001");

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            verify(submissionStorageService).deleteSubmissionFiles("stored/55/s2210001");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void stubGeneratedSubmissionIds() {
        AtomicLong sequence = new AtomicLong(100L);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            if (submission.getId() == null) {
                submission.setId(sequence.getAndIncrement());
            }
            return submission;
        });
    }

    private Assignment assignment(Long assignmentId, GradingMode gradingMode) {
        Semester semester = new Semester();
        semester.setId(7L);

        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setSemester(semester);
        assignment.setGradingMode(gradingMode);
        assignment.setAssignmentName("Phase 3 Assignment");
        return assignment;
    }

    private Submission submission(
            Long submissionId,
            Assignment assignment,
            String submitterName,
            String archiveFileName,
            SubmissionStatus status,
            String storagePath) {
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setAssignment(assignment);
        submission.setSubmitterName(submitterName);
        submission.setArchiveFileName(archiveFileName);
        submission.setStatus(status);
        submission.setStoragePath(storagePath);
        submission.setFileCount(1);
        return submission;
    }

    private Batch batch(Long batchId, Assignment assignment, BatchStatus status) {
        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setAssignment(assignment);
        batch.setStatus(status);
        batch.setQueueCapacity(8);
        batch.setWorkerCount(2);
        batch.setTotalSubmissions(1);
        batch.setGradeableSubmissionCount(1);
        batch.setExcludedSubmissionCount(0);
        return batch;
    }

    private GradingResult gradingResult(Submission submission) {
        GradingResult result = new GradingResult();
        result.setSubmission(submission);
        result.setStatus(GradingResultStatus.PENDING);
        result.setGradingMode(GradingMode.JAVA_CORE);
        return result;
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

    private byte[] zipOfRepeatedEntry(String name, byte fillByte, int totalBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        java.util.Arrays.fill(buffer, fillByte);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry(name));
            int remaining = totalBytes;
            while (remaining > 0) {
                int chunkSize = Math.min(remaining, buffer.length);
                zipOutputStream.write(buffer, 0, chunkSize);
                remaining -= chunkSize;
            }
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }

    private byte[] zipWithEntryCount(int entryCount) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (int i = 0; i < entryCount; i++) {
                zipOutputStream.putNextEntry(new ZipEntry("s2210001/File" + i + ".java"));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}

