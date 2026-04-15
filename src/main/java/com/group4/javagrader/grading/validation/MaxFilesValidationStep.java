package com.group4.javagrader.grading.validation;

import com.group4.javagrader.grading.context.SubmissionValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(10)
public class MaxFilesValidationStep implements SubmissionValidationStep {

    private final int maxFiles;

    public MaxFilesValidationStep() {
        this(200);
    }

    public MaxFilesValidationStep(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    @Override
    public Optional<SubmissionValidationFailure> validate(SubmissionValidationContext context) {
        if (context.getFiles().size() > maxFiles) {
            return Optional.of(new SubmissionValidationFailure(
                    "MAX_FILES_EXCEEDED",
                    "Submission contains more than " + maxFiles + " files."));
        }
        return Optional.empty();
    }
}
