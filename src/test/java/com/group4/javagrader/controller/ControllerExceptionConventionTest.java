package com.group4.javagrader.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerExceptionConventionTest {

    private static final Path CONTROLLER_DIRECTORY = Path.of("src/main/java/com/group4/javagrader/controller");
    private static final Pattern GENERIC_EXCEPTION_CATCH = Pattern.compile(
            "catch\\s*\\([^)]*(IllegalArgumentException|IllegalStateException)");

    @Test
    void controllersHandleDomainExceptionsInsteadOfGenericRuntimeExceptions() throws IOException {
        List<Path> controllerSources = controllerSources();

        assertThat(controllerSources)
                .describedAs("Controller sources should exist")
                .isNotEmpty();

        assertThat(controllerSources)
                .describedAs("Controller catch blocks should use DomainException or concrete domain exceptions")
                .allSatisfy(path -> assertThat(read(path))
                        .doesNotContainPattern(GENERIC_EXCEPTION_CATCH));
    }

    private static List<Path> controllerSources() throws IOException {
        try (Stream<Path> paths = Files.list(CONTROLLER_DIRECTORY)) {
            return paths
                    .filter(path -> path.toString().endsWith("Controller.java"))
                    .toList();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
