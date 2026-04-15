package com.group4.javagrader.service;

import com.group4.javagrader.dto.SubmissionFileView;
import com.group4.javagrader.dto.SubmissionUploadGuide;
import com.group4.javagrader.dto.SubmissionUploadSummary;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Submission;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SubmissionService {

    SubmissionUploadSummary uploadArchive(Long assignmentId, MultipartFile archiveFile);

    SubmissionUploadGuide getUploadGuide();

    List<Submission> findByAssignmentId(Long assignmentId);

    Optional<Assignment> findAssignment(Long assignmentId);

    Optional<Submission> findById(Long submissionId);

    List<SubmissionFileView> loadSubmissionFilesForViewing(Long submissionId);

    boolean canFixStructure(Submission submission);

    /**
     * Returns a map from original relative path to suggested repaired path.
     * The map may mark files for deletion by mapping them to an empty string.
     * Files that should remain unchanged are not included in the map.
     */
    Map<String, String> buildSuggestedPaths(Long submissionId);

    void fixSubmissionStructure(Long submissionId, Map<String, String> pathMappings, List<String> deletedPaths);
}
