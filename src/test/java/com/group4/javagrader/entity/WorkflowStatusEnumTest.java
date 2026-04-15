package com.group4.javagrader.entity;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.WorkspaceProblemForm;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowStatusEnumTest {

    @Test
    void submissionHelpersUseEnumStatuses() {
        Submission submission = new Submission();

        submission.setStatus(SubmissionStatus.VALIDATED);
        assertThat(submission.isValidated()).isTrue();
        assertThat(submission.isRejected()).isFalse();

        submission.setStatus(SubmissionStatus.REJECTED);
        assertThat(submission.isRejected()).isTrue();
        assertThat(submission.isValidated()).isFalse();
    }

    @Test
    void batchHelpersUseEnumStatuses() {
        Batch batch = new Batch();

        batch.setStatus(BatchStatus.PRECHECKED);
        assertThat(batch.canStart()).isTrue();
        assertThat(batch.isReadyForGrading()).isFalse();

        batch.setStatus(BatchStatus.READY_FOR_GRADING);
        assertThat(batch.isReadyForGrading()).isTrue();
    }

    @Test
    void gradingResultHelpersUseEnumStatuses() {
        GradingResult gradingResult = new GradingResult();

        gradingResult.setStatus(GradingResultStatus.PENDING);
        assertThat(gradingResult.isTerminal()).isFalse();

        gradingResult.setStatus(GradingResultStatus.DONE);
        assertThat(gradingResult.isTerminal()).isTrue();
        assertThat(gradingResult.isSuccessful()).isTrue();
    }

    @Test
    void gradingModeHelpersUseEnumModes() {
        Assignment assignment = new Assignment();
        assignment.setGradingMode(GradingMode.JAVA_CORE);
        assertThat(assignment.isJavaCoreMode()).isTrue();
        assertThat(assignment.isOopMode()).isFalse();

        GradingResult gradingResult = new GradingResult();
        gradingResult.setGradingMode(GradingMode.OOP);
        assertThat(gradingResult.isOopMode()).isTrue();
    }

    @Test
    void assignmentAndProblemConfigHelpersUseEnums() {
        Assignment assignment = new Assignment();
        assignment.setAssignmentType(AssignmentType.WEEKLY);
        assignment.setOutputNormalizationPolicy(OutputNormalizationPolicy.TRIM_ALL);

        Problem problem = new Problem();
        problem.setInputMode(InputMode.FILE);
        problem.setOutputComparisonMode(OutputComparisonMode.IGNORE_WHITESPACE);

        assertThat(assignment.isWeeklyAssignmentType()).isTrue();
        assertThat(assignment.isCustomAssignmentType()).isFalse();
        assertThat(assignment.getOutputNormalizationPolicy().toComparisonMode()).isEqualTo(OutputComparisonMode.TRIM_ALL);
        assertThat(problem.isFileInputMode()).isTrue();
        assertThat(problem.getOutputComparisonMode()).isEqualTo(OutputComparisonMode.IGNORE_WHITESPACE);
    }

    @Test
    void configTypesDoNotExposeStringBasedSetters() {
        assertThat(hasStringSetter(Assignment.class, "setAssignmentType")).isFalse();
        assertThat(hasStringSetter(Assignment.class, "setOutputNormalizationPolicy")).isFalse();
        assertThat(hasStringSetter(Problem.class, "setInputMode")).isFalse();
        assertThat(hasStringSetter(Problem.class, "setOutputComparisonMode")).isFalse();

        assertThat(hasStringSetter(AssignmentForm.class, "setAssignmentType")).isFalse();
        assertThat(hasStringSetter(AssignmentForm.class, "setGradingMode")).isFalse();
        assertThat(hasStringSetter(AssignmentForm.class, "setOutputNormalizationPolicy")).isFalse();
        assertThat(hasStringSetter(AssignmentForm.class, "setInputMode")).isFalse();

        assertThat(hasStringSetter(ProblemForm.class, "setInputMode")).isFalse();
        assertThat(hasStringSetter(ProblemForm.class, "setOutputComparisonMode")).isFalse();

        assertThat(hasStringSetter(WorkspaceProblemForm.class, "setInputMode")).isFalse();
        assertThat(hasStringSetter(WorkspaceProblemForm.class, "setOutputComparisonMode")).isFalse();

        assertThat(hasSingleStringMethod(GradingMode.class, "from")).isFalse();
    }

    private boolean hasStringSetter(Class<?> type, String methodName) {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(Method::getParameterTypes)
                .anyMatch(parameterTypes -> parameterTypes.length == 1 && parameterTypes[0] == String.class);
    }

    private boolean hasSingleStringMethod(Class<?> type, String methodName) {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(Method::getParameterTypes)
                .anyMatch(parameterTypes -> parameterTypes.length == 1 && parameterTypes[0] == String.class);
    }
}
