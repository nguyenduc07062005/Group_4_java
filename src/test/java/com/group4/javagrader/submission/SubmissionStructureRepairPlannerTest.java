package com.group4.javagrader.submission;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.service.impl.submission.SubmissionStructureRepairPlanner;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionStructureRepairPlannerTest {

    private final SubmissionStructureRepairPlanner planner = new SubmissionStructureRepairPlanner();

    @Test
    void buildRepairCandidateRehomesJavaCoreMainFile() {
        Assignment assignment = assignment(GradingMode.JAVA_CORE);
        List<SubmissionFile> files = List.of(
                file("nested/src/Main.java", "public class Main { public static void main(String[] args) {} }"),
                file("nested/src/Notes.txt", "ignore"));

        Optional<List<SubmissionFile>> repairCandidate = planner.buildRepairCandidate(assignment, files);

        assertThat(repairCandidate).isPresent();
        assertThat(repairCandidate.orElseThrow())
                .extracting(SubmissionFile::relativePath)
                .containsExactly("Main.java");
    }

    @Test
    void buildRepairCandidateRehomesOopSourceIntoPackagePath() {
        Assignment assignment = assignment(GradingMode.OOP);
        List<SubmissionFile> files = List.of(
                file("wrong/place/App.java", "package com.example.demo; public class App {}"));

        Optional<List<SubmissionFile>> repairCandidate = planner.buildRepairCandidate(assignment, files);

        assertThat(repairCandidate).isPresent();
        assertThat(repairCandidate.orElseThrow())
                .extracting(SubmissionFile::relativePath)
                .containsExactly("src/com/example/demo/App.java");
    }

    @Test
    void buildSuggestedPathsMarksUnmatchedFilesForDeletion() {
        List<SubmissionFile> originalFiles = List.of(
                file("deep/Main.java", "public class Main { public static void main(String[] args) {} }"),
                file("deep/readme.txt", "notes"));
        List<SubmissionFile> repairedFiles = List.of(
                file("Main.java", "public class Main { public static void main(String[] args) {} }"));

        Map<String, String> suggestedPaths = planner.buildSuggestedPaths(originalFiles, repairedFiles);

        assertThat(suggestedPaths).containsEntry("deep/Main.java", "Main.java");
        assertThat(suggestedPaths).containsEntry("deep/readme.txt", "");
    }

    private Assignment assignment(GradingMode gradingMode) {
        Assignment assignment = new Assignment();
        assignment.setGradingMode(gradingMode);
        return assignment;
    }

    private SubmissionFile file(String relativePath, String content) {
        return new SubmissionFile(relativePath, content.getBytes(StandardCharsets.UTF_8));
    }
}
