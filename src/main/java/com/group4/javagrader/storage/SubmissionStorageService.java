package com.group4.javagrader.storage;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.grading.context.SubmissionFile;

import java.util.List;

public interface SubmissionStorageService {

    String storeSubmissionFiles(Assignment assignment, Long submissionId, String submitterName, List<SubmissionFile> files);

    List<SubmissionFile> loadSubmissionFiles(String storagePath);

    void deleteSubmissionFiles(String storagePath);
}

