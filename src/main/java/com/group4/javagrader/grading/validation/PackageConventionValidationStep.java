package com.group4.javagrader.grading.validation;

import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.context.SubmissionValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(40)
public class PackageConventionValidationStep implements SubmissionValidationStep {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\s*;");
    private static final List<String> SOURCE_ROOT_PREFIXES = List.of("src/main/java/", "src/");

    @Override
    public Optional<SubmissionValidationFailure> validate(SubmissionValidationContext context) {
        if (context.getGradingMode() != GradingMode.OOP) {
            return Optional.empty();
        }

        for (SubmissionFile file : context.getFiles()) {
            if (!file.relativePath().endsWith(".java")) {
                continue;
            }

            String normalizedPath = stripKnownSourceRoot(file.relativePath());
            int lastSlash = normalizedPath.lastIndexOf('/');
            String expectedPackagePath = lastSlash >= 0 ? normalizedPath.substring(0, lastSlash) : "";
            String declaredPackagePath = extractDeclaredPackagePath(file);

            if (!declaredPackagePath.isBlank() && !declaredPackagePath.equals(expectedPackagePath)) {
                return Optional.of(new SubmissionValidationFailure(
                        "PACKAGE_PATH_MISMATCH",
                        "Declared package does not match the folder structure for " + file.relativePath()));
            }

            if (declaredPackagePath.isBlank() && !expectedPackagePath.isBlank()) {
                return Optional.of(new SubmissionValidationFailure(
                        "PACKAGE_DECLARATION_MISSING",
                        "Java file in a package folder must declare its package: " + file.relativePath()));
            }
        }
        return Optional.empty();
    }

    private String stripKnownSourceRoot(String relativePath) {
        for (String prefix : SOURCE_ROOT_PREFIXES) {
            if (relativePath.startsWith(prefix)) {
                return relativePath.substring(prefix.length());
            }
        }
        return relativePath;
    }

    private String extractDeclaredPackagePath(SubmissionFile file) {
        String source = new String(file.content(), StandardCharsets.UTF_8);
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replace('.', '/');
    }
}
