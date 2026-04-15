package com.group4.javagrader.grading.validation;

import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.context.SubmissionValidationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(20)
public class MaxDepthValidationStep implements SubmissionValidationStep {

    private final int maxDepth;

    public MaxDepthValidationStep() {
        this(8);
    }

    public MaxDepthValidationStep(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    public Optional<SubmissionValidationFailure> validate(SubmissionValidationContext context) {
        for (SubmissionFile file : context.getFiles()) {
            int depth = file.relativePath().split("/").length;
            if (depth > maxDepth) {
                return Optional.of(new SubmissionValidationFailure(
                        "MAX_DEPTH_EXCEEDED",
                        "Submission path is deeper than the allowed limit: " + file.relativePath()));
            }
        }
        return Optional.empty();
    }
}
