package com.group4.javagrader.submission;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionUploadAssetTest {

    @Test
    void asyncUploadRefreshReinitializesAlpineForFlashAlerts() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/submission/upload.html"));

        assertThat(template)
                .contains("currentMain.innerHTML = nextMain.innerHTML;")
                .contains("data-submission-upload-target")
                .contains("nextMain.dataset.submissionUploadTarget")
                .contains("window.Alpine.initTree(currentMain)");
    }
}
