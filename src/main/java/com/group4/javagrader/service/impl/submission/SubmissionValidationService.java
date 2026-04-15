package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.dto.SubmissionUploadGuide;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.context.SubmissionValidationContext;
import com.group4.javagrader.grading.validation.AllowedExtensionValidationStep;
import com.group4.javagrader.grading.validation.MaxDepthValidationStep;
import com.group4.javagrader.grading.validation.MaxFilesValidationStep;
import com.group4.javagrader.grading.validation.SubmissionValidationFailure;
import com.group4.javagrader.grading.validation.SubmissionValidationStep;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class SubmissionValidationService {

    private final List<SubmissionValidationStep> validationSteps;

    public SubmissionValidationService(List<SubmissionValidationStep> validationSteps) {
        this.validationSteps = List.copyOf(validationSteps);
    }

    public SubmissionUploadGuide buildUploadGuide(long maxArchiveSizeBytes) {
        return new SubmissionUploadGuide(
                maxArchiveSizeBytes / (1024L * 1024L),
                resolveMaxFilesPerSubmission(),
                resolveMaxFolderDepth(),
                resolveAllowedExtensions());
    }

    public Optional<SubmissionValidationFailure> validate(
            Assignment assignment,
            String submitterName,
            List<SubmissionFile> files) {
        SubmissionValidationContext context = new SubmissionValidationContext(
                assignment.getGradingMode(),
                submitterName,
                files);

        for (SubmissionValidationStep validationStep : validationSteps) {
            Optional<SubmissionValidationFailure> failure = validationStep.validate(context);
            if (failure.isPresent()) {
                return failure;
            }
        }

        return Optional.empty();
    }

    private int resolveMaxFilesPerSubmission() {
        return validationSteps.stream()
                .filter(MaxFilesValidationStep.class::isInstance)
                .map(MaxFilesValidationStep.class::cast)
                .findFirst()
                .map(MaxFilesValidationStep::getMaxFiles)
                .orElse(200);
    }

    private int resolveMaxFolderDepth() {
        return validationSteps.stream()
                .filter(MaxDepthValidationStep.class::isInstance)
                .map(MaxDepthValidationStep.class::cast)
                .findFirst()
                .map(MaxDepthValidationStep::getMaxDepth)
                .orElse(8);
    }

    private List<String> resolveAllowedExtensions() {
        return validationSteps.stream()
                .filter(AllowedExtensionValidationStep.class::isInstance)
                .map(AllowedExtensionValidationStep.class::cast)
                .findFirst()
                .map(AllowedExtensionValidationStep::getAllowedExtensions)
                .orElseGet(Set::of)
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
