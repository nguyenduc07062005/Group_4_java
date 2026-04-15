package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.grading.context.SubmissionFile;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SubmissionArchiveParser {

    private static final long MAX_ARCHIVE_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final long MAX_TOTAL_EXTRACTED_BYTES = 50L * 1024L * 1024L;
    private static final int MAX_ZIP_ENTRY_COUNT = 10_000;
    private static final int MAX_SUBMITTER_COUNT = 1_000;
    private static final Pattern WINDOWS_DRIVE_PATTERN = Pattern.compile("^[a-zA-Z]:.*");
    private static final Pattern INVALID_PATH_CHARS_PATTERN = Pattern.compile("[<>:\\\"|?*]");

    public long getMaxArchiveSizeBytes() {
        return MAX_ARCHIVE_SIZE_BYTES;
    }

    public void validateArchiveFile(MultipartFile archiveFile) {
        if (archiveFile == null || archiveFile.getOriginalFilename() == null || archiveFile.getOriginalFilename().isBlank()) {
            throw new InputValidationException("Submission archive is required.");
        }
        if (archiveFile.isEmpty()) {
            throw new InputValidationException("Submission archive must not be empty.");
        }
        if (archiveFile.getSize() > MAX_ARCHIVE_SIZE_BYTES) {
            throw new InputValidationException("Submission archive exceeds the 20 MB size limit.");
        }

        String fileName = StringUtils.cleanPath(archiveFile.getOriginalFilename());
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new InputValidationException("Submission archive must be a .zip file.");
        }
    }

    public Map<String, List<SubmissionFile>> parseArchive(MultipartFile archiveFile) {
        Map<String, List<SubmissionFile>> groupedSubmissions = new LinkedHashMap<>();
        Map<String, String> canonicalSubmitterNames = new LinkedHashMap<>();
        Map<String, java.util.Set<String>> seenRelativePathsBySubmitter = new LinkedHashMap<>();
        long totalExtractedBytes = 0L;

        try (InputStream inputStream = archiveFile.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            int zipEntryCount = 0;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                zipEntryCount++;
                if (zipEntryCount > MAX_ZIP_ENTRY_COUNT) {
                    throw new InputValidationException("Submission archive contains too many ZIP entries.");
                }

                List<String> segments = normalizeEntrySegments(entry.getName());
                if (!entry.isDirectory()) {
                    if (segments.size() < 2) {
                        throw new InputValidationException("Submission archive must place files inside a top-level student folder.");
                    }

                    String submitterName = normalizeSubmitterFolderName(segments.get(0));
                    String normalizedSubmitterName = normalizeSubmitter(submitterName);
                    String existingSubmitterName = canonicalSubmitterNames.putIfAbsent(
                            normalizedSubmitterName,
                            submitterName);
                    if (existingSubmitterName != null && !existingSubmitterName.equals(submitterName)) {
                        throw new InputValidationException(
                                "Submission archive contains duplicate student folders that differ only by case or spacing.");
                    }
                    if (!groupedSubmissions.containsKey(submitterName)
                            && groupedSubmissions.size() >= MAX_SUBMITTER_COUNT) {
                        throw new InputValidationException("Submission archive contains too many student folders.");
                    }
                    String relativePath = String.join("/", segments.subList(1, segments.size()));
                    String normalizedRelativePathKey = relativePath.toLowerCase(Locale.ROOT);
                    boolean added = seenRelativePathsBySubmitter
                            .computeIfAbsent(submitterName, ignored -> new java.util.LinkedHashSet<>())
                            .add(normalizedRelativePathKey);
                    if (!added) {
                        throw new InputValidationException(
                                "Submission archive contains duplicate file paths after path normalization.");
                    }
                    byte[] content = readEntryBytes(zipInputStream, totalExtractedBytes);
                    totalExtractedBytes += content.length;
                    groupedSubmissions.computeIfAbsent(submitterName, ignored -> new ArrayList<>())
                            .add(new SubmissionFile(relativePath, content));
                }
                zipInputStream.closeEntry();
            }
        } catch (InputValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new InputValidationException("Submission archive is not a valid ZIP file.", ex);
        }

        if (groupedSubmissions.isEmpty()) {
            throw new InputValidationException("Submission archive does not contain any files.");
        }

        return groupedSubmissions;
    }

    private byte[] readEntryBytes(ZipInputStream zipInputStream, long extractedBytesSoFar) throws IOException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        long totalExtractedBytes = extractedBytesSoFar;
        while ((read = zipInputStream.read(buffer)) != -1) {
            totalExtractedBytes += read;
            if (totalExtractedBytes > MAX_TOTAL_EXTRACTED_BYTES) {
                throw new InputValidationException("Submission archive expands beyond the 50 MB safety limit.");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private List<String> normalizeEntrySegments(String entryName) {
        if (!StringUtils.hasText(entryName)) {
            throw new InputValidationException("Submission archive contains an unsafe path.");
        }

        String normalized = entryName.replace('\\', '/').trim();
        if (normalized.startsWith("/") || WINDOWS_DRIVE_PATTERN.matcher(normalized).matches()) {
            throw new InputValidationException("Submission archive contains an unsafe path.");
        }

        String[] rawSegments = normalized.split("/");
        List<String> segments = new ArrayList<>();
        for (String rawSegment : rawSegments) {
            if (!StringUtils.hasText(rawSegment) || ".".equals(rawSegment)) {
                continue;
            }
            if ("..".equals(rawSegment) || INVALID_PATH_CHARS_PATTERN.matcher(rawSegment).find()) {
                throw new InputValidationException("Submission archive contains an unsafe path.");
            }
            segments.add(rawSegment.trim());
        }

        if (segments.isEmpty()) {
            throw new InputValidationException("Submission archive contains an unsafe path.");
        }

        return segments;
    }

    private String normalizeSubmitterFolderName(String rawSubmitterName) {
        String normalizedSubmitterName = rawSubmitterName == null ? "" : rawSubmitterName.trim();
        if (!StringUtils.hasText(normalizedSubmitterName)) {
            throw new InputValidationException("Submission archive contains an unsafe path.");
        }
        return normalizedSubmitterName;
    }

    private String normalizeSubmitter(String submitterName) {
        return submitterName == null ? "" : submitterName.trim().toLowerCase(Locale.ROOT);
    }
}
