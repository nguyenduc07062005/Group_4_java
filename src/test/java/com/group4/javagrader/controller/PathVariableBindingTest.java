package com.group4.javagrader.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class PathVariableBindingTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            AssignmentController.class,
            AssignmentWorkspaceController.class,
            AutoGradeController.class,
            BatchController.class,
            CourseController.class,
            GradingWorkspaceController.class,
            PlagiarismController.class,
            ReportController.class,
            ResultController.class,
            SemesterController.class,
            SubmissionController.class,
            TestCaseController.class);

    @Test
    void allPathVariablesAreExplicitlyNamed() {
        Map<String, String> unnamedPathVariables = new LinkedHashMap<>();

        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                    if (pathVariable == null) {
                        continue;
                    }

                    String explicitName = resolveExplicitName(pathVariable);
                    if (explicitName == null) {
                        unnamedPathVariables.put(
                                controller.getSimpleName() + "#" + method.getName() + "(" + parameter.getType().getSimpleName() + ")",
                                "missing explicit @PathVariable name");
                    }
                }
            }
        }

        assertThat(unnamedPathVariables)
                .as("Every @PathVariable should use an explicit name to keep route binding stable")
                .isEmpty();
    }

    private String resolveExplicitName(PathVariable pathVariable) {
        if (pathVariable == null) {
            return null;
        }

        String value = pathVariable.value();
        if (value != null && !value.isBlank()) {
            return value;
        }

        String name = pathVariable.name();
        if (name != null && !name.isBlank()) {
            return name;
        }

        return null;
    }
}
