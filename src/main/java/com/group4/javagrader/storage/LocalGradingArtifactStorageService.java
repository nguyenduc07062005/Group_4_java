package com.group4.javagrader.storage;

import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.grading.context.SubmissionFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Service
public class LocalGradingArtifactStorageService implements GradingArtifactStorageService {

    private final Path gradingRoot;

    public LocalGradingArtifactStorageService(@Value("${app.storage.grading-root:./data/grading}") String gradingRoot) {
        this.gradingRoot = Paths.get(gradingRoot).toAbsolutePath().normalize();
    }

    @Override
    public PreparedWorkspace prepareWorkspace(Batch batch, Submission submission, List<SubmissionFile> files) {
        Path artifactRoot = gradingRoot
                .resolve(String.valueOf(batch.getAssignment().getSemester().getId()))
                .resolve(String.valueOf(batch.getAssignment().getId()))
                .resolve(String.valueOf(batch.getId()))
                .resolve(String.valueOf(submission.getId()))
                .normalize();
        Path sourceRoot = artifactRoot.resolve("workspace").resolve("src");
        Path classesRoot = artifactRoot.resolve("workspace").resolve("classes");

        if (!artifactRoot.startsWith(gradingRoot)) {
            throw new IllegalArgumentException("Grading artifact path could not be resolved safely.");
        }

        try {
            Files.createDirectories(sourceRoot);
            Files.createDirectories(classesRoot);
            for (SubmissionFile file : files) {
                Path targetPath = resolveRelativePath(sourceRoot, file.relativePath());
                Files.createDirectories(targetPath.getParent());
                Files.write(targetPath, file.content());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Grading workspace could not be prepared.");
        }

        return new PreparedWorkspace(artifactRoot, sourceRoot, classesRoot);
    }

    @Override
    public void writeTextArtifact(Path artifactRoot, String fileName, String content) {
        try {
            Files.createDirectories(artifactRoot);
            Files.writeString(artifactRoot.resolve(fileName), content != null ? content : "", StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Grading artifact could not be written.");
        }
    }

    @Override
    public void cleanupWorkspace(PreparedWorkspace workspace) {
        Path workspaceRoot = workspace.artifactRoot().resolve("workspace");
        if (!workspaceRoot.startsWith(gradingRoot)) {
            return;
        }

        try (var paths = Files.walk(workspaceRoot)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best effort cleanup so grading results remain accessible even if temp deletion fails.
                        }
                    });
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    private Path resolveRelativePath(Path sourceRoot, String relativePath) {
        Path current = sourceRoot;
        for (String segment : relativePath.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Submission file path is invalid.");
            }
            current = current.resolve(segment);
        }

        Path normalized = current.normalize();
        if (!normalized.startsWith(sourceRoot)) {
            throw new IllegalArgumentException("Submission file path escaped the source directory.");
        }
        return normalized;
    }
}
