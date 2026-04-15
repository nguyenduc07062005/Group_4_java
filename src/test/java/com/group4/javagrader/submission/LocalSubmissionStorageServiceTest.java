package com.group4.javagrader.submission;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.storage.LocalSubmissionStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalSubmissionStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void failedReplacementKeepsPreviousStoredSnapshotIntact() {
        LocalSubmissionStorageService storageService = new LocalSubmissionStorageService(tempDir.toString());
        Assignment assignment = assignment(15L, 7L);

        String storagePath = storageService.storeSubmissionFiles(
                assignment,
                99L,
                "s2213001",
                List.of(
                        new SubmissionFile("Main.java", "class Main {}".getBytes()),
                        new SubmissionFile("OldHelper.java", "class OldHelper {}".getBytes())));

        assertThatThrownBy(() -> storageService.storeSubmissionFiles(
                assignment,
                99L,
                "s2213001",
                List.of(
                        new SubmissionFile("src/Main.java", "class Main {}".getBytes()),
                        new SubmissionFile("src/Main.java/Helper.java", "class Helper {}".getBytes()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not be stored");

        assertThat(storageService.loadSubmissionFiles(storagePath))
                .extracting(SubmissionFile::relativePath)
                .containsExactly("Main.java", "OldHelper.java");
    }

    @Test
    void invalidRelativePathCleansStagingDirectory() throws Exception {
        LocalSubmissionStorageService storageService = new LocalSubmissionStorageService(tempDir.toString());
        Assignment assignment = assignment(15L, 7L);

        assertThatThrownBy(() -> storageService.storeSubmissionFiles(
                assignment,
                99L,
                "s2213001",
                List.of(new SubmissionFile("../evil.java", "class Evil {}".getBytes()))))
                .isInstanceOf(IllegalArgumentException.class);

        try (Stream<Path> paths = java.nio.file.Files.walk(tempDir)) {
            assertThat(paths)
                    .noneMatch(path -> path.getFileName() != null
                            && path.getFileName().toString().contains(".staging-"));
        }
    }

    private Assignment assignment(Long assignmentId, Long semesterId) {
        Semester semester = new Semester();
        semester.setId(semesterId);

        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setSemester(semester);
        return assignment;
    }
}
