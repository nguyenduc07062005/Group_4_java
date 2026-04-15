package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.grading.context.SubmissionFile;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubmissionStructureRepairPlanner {

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\s*;");
    private static final Pattern TYPE_DECLARATION_PATTERN =
            Pattern.compile("(?m)^\\s*(?:public\\s+)?(?:class|interface|enum|record)\\s+([A-Za-z_]\\w*)");
    private static final Pattern MAIN_METHOD_PATTERN =
            Pattern.compile("public\\s+static\\s+void\\s+main\\s*\\(");

    public Optional<List<SubmissionFile>> buildRepairCandidate(Assignment assignment, List<SubmissionFile> files) {
        if (assignment == null || files == null || files.isEmpty()) {
            return Optional.empty();
        }
        if (assignment.isJavaCoreMode()) {
            return buildJavaCoreRepairCandidate(files);
        }
        if (assignment.isOopMode()) {
            return buildOopRepairCandidate(files);
        }
        return Optional.empty();
    }

    public Map<String, String> buildSuggestedPaths(List<SubmissionFile> originalFiles, List<SubmissionFile> repairedFiles) {
        Map<String, String> suggestedPaths = new LinkedHashMap<>();
        if (originalFiles == null || originalFiles.isEmpty() || repairedFiles == null || repairedFiles.isEmpty()) {
            return suggestedPaths;
        }

        Map<String, String> contentHashToRepairedPath = new LinkedHashMap<>();
        for (SubmissionFile repaired : repairedFiles) {
            String hash = java.util.Arrays.hashCode(repaired.content()) + "_" + repaired.content().length;
            contentHashToRepairedPath.put(hash, repaired.relativePath());
        }

        java.util.Set<String> matchedRepairedPaths = new java.util.LinkedHashSet<>();
        for (SubmissionFile original : originalFiles) {
            String hash = java.util.Arrays.hashCode(original.content()) + "_" + original.content().length;
            String repairedPath = contentHashToRepairedPath.get(hash);
            if (repairedPath != null && !matchedRepairedPaths.contains(repairedPath)) {
                matchedRepairedPaths.add(repairedPath);
                if (!original.relativePath().equals(repairedPath)) {
                    suggestedPaths.put(original.relativePath(), repairedPath);
                }
            } else {
                suggestedPaths.put(original.relativePath(), "");
            }
        }

        return suggestedPaths;
    }

    private Optional<List<SubmissionFile>> buildJavaCoreRepairCandidate(List<SubmissionFile> files) {
        return files.stream()
                .map(this::inspectSourceCandidate)
                .filter(SourceCandidate::sourceLike)
                .filter(candidate -> candidate.packagePath().isBlank())
                .filter(candidate -> "Main".equals(candidate.typeName())
                        || (candidate.typeName() == null && "Main".equals(candidate.baseName())))
                .findFirst()
                .map(candidate -> List.of(new SubmissionFile("Main.java", candidate.content())));
    }

    private Optional<List<SubmissionFile>> buildOopRepairCandidate(List<SubmissionFile> files) {
        return files.stream()
                .map(this::inspectSourceCandidate)
                .filter(SourceCandidate::sourceLike)
                .findFirst()
                .map(candidate -> {
                    String fileName = sanitizeJavaIdentifier(candidate.typeName(), candidate.baseName()) + ".java";
                    String relativePath = candidate.packagePath().isBlank()
                            ? "src/" + fileName
                            : "src/" + candidate.packagePath() + "/" + fileName;
                    return List.of(new SubmissionFile(relativePath, candidate.content()));
                });
    }

    private SourceCandidate inspectSourceCandidate(SubmissionFile file) {
        String source = new String(file.content(), StandardCharsets.UTF_8);
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
        String packagePath = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') : "";

        Matcher typeMatcher = TYPE_DECLARATION_PATTERN.matcher(source);
        String typeName = typeMatcher.find() ? typeMatcher.group(1) : null;

        boolean sourceLike = file.relativePath().endsWith(".java")
                || !packagePath.isBlank()
                || typeName != null
                || MAIN_METHOD_PATTERN.matcher(source).find();

        return new SourceCandidate(
                file.content(),
                packagePath,
                typeName,
                sanitizeJavaIdentifier(stripExtension(fileNameOf(file.relativePath())), "SubmissionFile"),
                sourceLike);
    }

    private String fileNameOf(String relativePath) {
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    private String sanitizeJavaIdentifier(String value, String fallback) {
        String candidate = value == null ? "" : value.replaceAll("[^A-Za-z0-9_]", "");
        if (candidate.isBlank()) {
            return fallback;
        }
        if (!Character.isJavaIdentifierStart(candidate.charAt(0))) {
            candidate = fallback + candidate;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < candidate.length(); index++) {
            char ch = candidate.charAt(index);
            builder.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
        }
        return builder.toString();
    }

    private record SourceCandidate(
            byte[] content,
            String packagePath,
            String typeName,
            String baseName,
            boolean sourceLike) {
    }
}
