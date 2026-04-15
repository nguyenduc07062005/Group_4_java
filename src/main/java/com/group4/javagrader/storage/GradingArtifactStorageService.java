package com.group4.javagrader.storage;

import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.grading.context.SubmissionFile;

import java.nio.file.Path;
import java.util.List;

public interface GradingArtifactStorageService {

    PreparedWorkspace prepareWorkspace(Batch batch, Submission submission, List<SubmissionFile> files);

    void writeTextArtifact(Path artifactRoot, String fileName, String content);

    void cleanupWorkspace(PreparedWorkspace workspace);

    record PreparedWorkspace(Path artifactRoot, Path sourceRoot, Path classesRoot) {
    }
}
