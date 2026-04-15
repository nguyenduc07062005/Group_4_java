package com.group4.javagrader.storage;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.grading.context.SubmissionFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class LocalSubmissionStorageService implements SubmissionStorageService {

    private final Path submissionsRoot;

    public LocalSubmissionStorageService(@Value("${app.storage.submissions-root:./data/submissions}") String submissionsRoot) {
        this.submissionsRoot = Paths.get(submissionsRoot).toAbsolutePath().normalize();
    }

    @Override
    public String storeSubmissionFiles(Assignment assignment, Long submissionId, String submitterName, List<SubmissionFile> files) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission id is required before files can be stored.");
        }

        Path targetRoot = submissionsRoot
                .resolve(String.valueOf(assignment.getSemester().getId()))
                .resolve(String.valueOf(assignment.getId()))
                .resolve(String.valueOf(submissionId))
                .normalize();

        if (!targetRoot.startsWith(submissionsRoot)) {
            throw new IllegalArgumentException("Submission storage path could not be resolved safely.");
        }

        Path stagingRoot = createSiblingPath(targetRoot, ".staging-");
        Path backupRoot = null;
        try {
            deleteRecursively(stagingRoot);
            Files.createDirectories(stagingRoot);
            for (SubmissionFile file : files) {
                Path targetPath = resolveRelativePath(stagingRoot, file.relativePath());
                Files.createDirectories(targetPath.getParent());
                Files.write(targetPath, file.content());
            }

            if (Files.exists(targetRoot)) {
                backupRoot = createSiblingPath(targetRoot, ".backup-");
                movePath(targetRoot, backupRoot);
            }

            movePath(stagingRoot, targetRoot);
            if (backupRoot != null) {
                deleteRecursively(backupRoot);
            }
        } catch (IOException ex) {
            cleanupQuietly(stagingRoot);
            restoreBackupIfNeeded(backupRoot, targetRoot);
            throw new IllegalArgumentException("Submission files could not be stored.");
        } catch (RuntimeException ex) {
            cleanupQuietly(stagingRoot);
            restoreBackupIfNeeded(backupRoot, targetRoot);
            throw ex;
        }

        return targetRoot.toString();
    }

    private Path createSiblingPath(Path targetRoot, String suffixPrefix) {
        Path sibling = targetRoot.resolveSibling(targetRoot.getFileName() + suffixPrefix + UUID.randomUUID());
        Path normalized = sibling.normalize();
        if (!normalized.startsWith(submissionsRoot)) {
            throw new IllegalArgumentException("Submission storage path could not be resolved safely.");
        }
        return normalized;
    }

    private void movePath(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private void deleteRecursively(Path targetRoot) throws IOException {
        if (!Files.exists(targetRoot)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(targetRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void restoreBackupIfNeeded(Path backupRoot, Path targetRoot) {
        if (backupRoot == null || !Files.exists(backupRoot) || Files.exists(targetRoot)) {
            return;
        }
        try {
            movePath(backupRoot, targetRoot);
        } catch (IOException ignored) {
            // Preserve the backup directory for manual recovery if restoration fails.
        }
    }

    private void cleanupQuietly(Path targetRoot) {
        try {
            deleteRecursively(targetRoot);
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }

    @Override
    public List<SubmissionFile> loadSubmissionFiles(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new IllegalArgumentException("Submission storage path is missing.");
        }

        Path targetRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!targetRoot.startsWith(submissionsRoot)) {
            throw new IllegalArgumentException("Submission storage path could not be resolved safely.");
        }
        if (!Files.exists(targetRoot)) {
            throw new IllegalArgumentException("Stored submission files could not be found.");
        }

        try (Stream<Path> paths = Files.walk(targetRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.naturalOrder())
                    .map(path -> toSubmissionFile(targetRoot, path))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Stored submission files could not be loaded.");
        }
    }

    @Override
    public void deleteSubmissionFiles(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }

        Path targetRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!targetRoot.startsWith(submissionsRoot)) {
            throw new IllegalArgumentException("Submission storage path could not be resolved safely.");
        }

        try {
            deleteRecursively(targetRoot);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Submission files could not be deleted.");
        }
    }

    private SubmissionFile toSubmissionFile(Path targetRoot, Path filePath) {
        try {
            return new SubmissionFile(
                    targetRoot.relativize(filePath).toString().replace('\\', '/'),
                    Files.readAllBytes(filePath));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Stored submission files could not be loaded.");
        }
    }

    private Path resolveRelativePath(Path targetRoot, String relativePath) {
        Path current = targetRoot;
        for (String segment : relativePath.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Submission file path is invalid.");
            }
            current = current.resolve(segment);
        }

        Path normalized = current.normalize();
        if (!normalized.startsWith(targetRoot)) {
            throw new IllegalArgumentException("Submission file path escaped the target directory.");
        }
        return normalized;
    }
}

