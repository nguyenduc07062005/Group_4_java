package com.group4.javagrader.dto;

import lombok.Getter;

/**
 * Read-only view of a file inside a submission, used for displaying
 * file structure in the fix-structure editor.
 */
@Getter
public class SubmissionFileView {

    private final String relativePath;
    private final long sizeBytes;
    private final String previewSnippet;
    private final String fileName;
    private final String directoryPath;

    public SubmissionFileView(String relativePath, long sizeBytes, String previewSnippet) {
        this.relativePath = relativePath;
        this.sizeBytes = sizeBytes;
        this.previewSnippet = previewSnippet;
        int lastSlash = relativePath.lastIndexOf('/');
        this.fileName = lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
        this.directoryPath = lastSlash >= 0 ? relativePath.substring(0, lastSlash) : "";
    }
}
