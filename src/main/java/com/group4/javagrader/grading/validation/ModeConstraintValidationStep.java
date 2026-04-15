package com.group4.javagrader.grading.validation;

import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.context.SubmissionValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(50)
public class ModeConstraintValidationStep implements SubmissionValidationStep {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\s*;");

    @Override
    public Optional<SubmissionValidationFailure> validate(SubmissionValidationContext context) {
        if (context.getGradingMode() == GradingMode.JAVA_CORE) {
            return validateJavaCore(context);
        }
        if (context.getGradingMode() == GradingMode.OOP) {
            return validateOop(context);
        }
        return Optional.empty();
    }

    private Optional<SubmissionValidationFailure> validateJavaCore(SubmissionValidationContext context) {
        boolean hasMainAtRoot = false;

        for (SubmissionFile file : context.getFiles()) {
            if (!file.relativePath().endsWith(".java")) {
                continue;
            }

            if (file.relativePath().contains("/")) {
                return Optional.of(new SubmissionValidationFailure(
                        "JAVA_CORE_FLAT_STRUCTURE_REQUIRED",
                        "Java Core submissions must keep Java source files at the submission root: " + file.relativePath()));
            }

            if (hasPackageDeclaration(file)) {
                return Optional.of(new SubmissionValidationFailure(
                        "JAVA_CORE_PACKAGE_NOT_ALLOWED",
                        "Java Core submissions must not declare a package declaration."));
            }

            if ("Main.java".equals(file.relativePath())) {
                hasMainAtRoot = true;
            }
        }

        if (!hasMainAtRoot) {
            return Optional.of(new SubmissionValidationFailure(
                    "JAVA_CORE_MISSING_MAIN",
                    "Java Core submissions must include Main.java at the submission root."));
        }

        return Optional.empty();
    }

    private Optional<SubmissionValidationFailure> validateOop(SubmissionValidationContext context) {
        boolean hasJavaFile = context.getFiles().stream().anyMatch(file -> file.relativePath().endsWith(".java"));
        if (!hasJavaFile) {
            return Optional.of(new SubmissionValidationFailure(
                    "OOP_JAVA_FILE_REQUIRED",
                    "OOP submissions must contain at least one Java source file."));
        }
        return Optional.empty();
    }

    private boolean hasPackageDeclaration(SubmissionFile file) {
        String source = new String(file.content(), StandardCharsets.UTF_8);
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find();
    }
}
