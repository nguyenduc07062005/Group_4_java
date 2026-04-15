package com.group4.javagrader.grading.validation;

import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.context.SubmissionValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
@Order(30)
public class AllowedExtensionValidationStep implements SubmissionValidationStep {

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "java", "txt", "md", "xml", "properties", "json", "yml", "yaml", "csv", "gradle", "kts");

    private final Set<String> allowedExtensions;

    public AllowedExtensionValidationStep() {
        this(DEFAULT_ALLOWED_EXTENSIONS);
    }

    public AllowedExtensionValidationStep(Set<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    @Override
    public Optional<SubmissionValidationFailure> validate(SubmissionValidationContext context) {
        for (SubmissionFile file : context.getFiles()) {
            String extension = extractExtension(file.relativePath());
            if (extension == null || !allowedExtensions.contains(extension)) {
                return Optional.of(new SubmissionValidationFailure(
                        "EXTENSION_NOT_ALLOWED",
                        "Submission contains a file type that is not allowed: " + file.relativePath()));
            }
        }
        return Optional.empty();
    }

    private String extractExtension(String relativePath) {
        int dotIndex = relativePath.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == relativePath.length() - 1) {
            return null;
        }
        return relativePath.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
