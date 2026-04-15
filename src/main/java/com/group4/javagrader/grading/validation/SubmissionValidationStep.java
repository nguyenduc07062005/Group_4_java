package com.group4.javagrader.grading.validation;

import com.group4.javagrader.grading.context.SubmissionValidationContext;

import java.util.Optional;

public interface SubmissionValidationStep {

    Optional<SubmissionValidationFailure> validate(SubmissionValidationContext context);
}
