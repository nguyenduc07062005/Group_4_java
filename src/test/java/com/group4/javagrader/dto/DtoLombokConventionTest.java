package com.group4.javagrader.dto;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DtoLombokConventionTest {

    private static final Path DTO_DIRECTORY = Path.of("src/main/java/com/group4/javagrader/dto");

    @Test
    void dtoTypesUseLombokInsteadOfManualAccessorsOrRecords() throws IOException {
        List<Path> dtoSources = dtoSources();

        assertThat(dtoSources)
                .describedAs("DTO sources should exist")
                .isNotEmpty();

        assertThat(dtoSources)
                .describedAs("DTO types should import Lombok")
                .allSatisfy(path -> assertThat(read(path)).contains("import lombok."));

        assertThat(dtoSources)
                .describedAs("DTO types should use Lombok classes instead of records")
                .allSatisfy(path -> assertThat(read(path)).doesNotContain("public record "));

        assertThat(dtoSources)
                .describedAs("DTO types should rely on Lombok for bean accessors")
                .allSatisfy(path -> assertThat(read(path))
                        .doesNotContainPattern("public\\s+\\w+(?:<[^>]+>)?\\s+get[A-Z]\\w*\\s*\\(")
                        .doesNotContainPattern("public\\s+void\\s+set[A-Z]\\w*\\s*\\("));
    }

    private static List<Path> dtoSources() throws IOException {
        try (Stream<Path> paths = Files.list(DTO_DIRECTORY)) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
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
