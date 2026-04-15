package com.group4.javagrader.grading.plagiarism;

import com.group4.javagrader.grading.context.SubmissionFile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SourceNormalizer {

    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("//.*?(\\R|$)");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public String normalize(List<SubmissionFile> files) {
        List<SubmissionFile> primaryFiles = files.stream()
                .filter(file -> file.relativePath().toLowerCase(Locale.ROOT).endsWith(".java"))
                .sorted(Comparator.comparing(SubmissionFile::relativePath))
                .toList();

        if (primaryFiles.isEmpty()) {
            primaryFiles = files.stream()
                    .sorted(Comparator.comparing(SubmissionFile::relativePath))
                    .toList();
        }

        return primaryFiles.stream()
                .map(file -> file.relativePath() + "\n" + normalizeSourceText(new String(file.content(), StandardCharsets.UTF_8)))
                .collect(Collectors.joining("\n"));
    }

    private String normalizeSourceText(String rawSource) {
        String withoutBlockComments = BLOCK_COMMENT_PATTERN.matcher(rawSource).replaceAll(" ");
        String withoutLineComments = LINE_COMMENT_PATTERN.matcher(withoutBlockComments).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(withoutLineComments)
                .replaceAll(" ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
