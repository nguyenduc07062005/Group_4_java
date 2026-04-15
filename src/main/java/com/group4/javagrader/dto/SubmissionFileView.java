package com.group4.javagrader.dto;

/**
 * Read-only view of a file inside a submission, used for displaying
 * file structure in the fix-structure editor.
 */
public class SubmissionFileView {

    private final String relativePath;
    private final long sizeBytes;
    private final String previewSnippet;

    public SubmissionFileView(String relativePath, long sizeBytes, String previewSnippet) {
        this.relativePath = relativePath;
        this.sizeBytes = sizeBytes;
        this.previewSnippet = previewSnippet;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getPreviewSnippet() {
        return previewSnippet;
    }

    /**
     * Returns the file name component only (e.g. "Main.java").
     */
    public String getFileName() {
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
    }

    /**
     * Returns the directory path component (e.g. "src/main/java/com/example"), or empty string if at root.
     */
    public String getDirectoryPath() {
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(0, lastSlash) : "";
    }
}
