package com.group4.javagrader.submission;

import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.service.impl.submission.SubmissionArchiveParser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionArchiveParserTest {

    private final SubmissionArchiveParser parser = new SubmissionArchiveParser();

    @Test
    void parseArchiveGroupsFilesByCanonicalSubmitterName() throws Exception {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        " s2210001 /Main.java", "public class Main {}",
                        "s2210001/src/Helper.java", "class Helper {}",
                        "s2210002/App.java", "public class App {}"));

        Map<String, List<SubmissionFile>> groupedSubmissions = parser.parseArchive(archiveFile);

        assertThat(groupedSubmissions).containsOnlyKeys("s2210001", "s2210002");
        assertThat(groupedSubmissions.get("s2210001"))
                .extracting(SubmissionFile::relativePath)
                .containsExactly("Main.java", "src/Helper.java");
    }

    @Test
    void parseArchiveRejectsDuplicateNormalizedPathsWithinSameSubmission() throws Exception {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.zip",
                "application/zip",
                zipOf(
                        "s2210001/src/Main.java", "public class Main {}",
                        "s2210001/src/./Main.java", "class Shadow {}"));

        assertThatThrownBy(() -> parser.parseArchive(archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining("duplicate file paths");
    }

    @Test
    void validateArchiveFileRejectsNonZipUpload() {
        MockMultipartFile archiveFile = new MockMultipartFile(
                "archiveFile",
                "submissions.rar",
                "application/octet-stream",
                "content".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.validateArchiveFile(archiveFile))
                .isInstanceOf(InputValidationException.class)
                .hasMessageContaining(".zip");
    }

    private byte[] zipOf(String... nameContentPairs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                zipOutputStream.putNextEntry(new ZipEntry(nameContentPairs[i]));
                zipOutputStream.write(nameContentPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}
