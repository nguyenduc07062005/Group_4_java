package com.group4.javagrader.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SubmissionUploadGuide {

    private final long maxArchiveSizeMb;
    private final int maxFilesPerSubmission;
    private final int maxFolderDepth;
    private final List<String> allowedExtensions;
}
