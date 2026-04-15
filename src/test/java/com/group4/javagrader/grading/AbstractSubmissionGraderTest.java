package com.group4.javagrader.grading;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.engine.AbstractSubmissionGrader;
import com.group4.javagrader.grading.engine.GradingJob;
import com.group4.javagrader.grading.engine.RuleCheckOutcome;
import com.group4.javagrader.grading.engine.SubmissionGradingOutcome;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.storage.GradingArtifactStorageService;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractSubmissionGraderTest {

    private static final String LARGE_LINE = "OUT".repeat(2048);
    private static final String STDERR_LINE = "ERR".repeat(2048);
    private static final int REPETITIONS = 48;

    @TempDir
    Path tempDir;

    @Test
    void gradeDrainsLargeStdoutAndStderrWithoutTimingOut() {
        Assignment assignment = new Assignment();
        assignment.setId(11L);
        assignment.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);

        Problem problem = new Problem();
        problem.setId(22L);
        problem.setAssignment(assignment);
        problem.setProblemOrder(1);
        problem.setTitle("Large Output");
        problem.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problem.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        problem.setMaxScore(new BigDecimal("10.00"));

        TestCase testCase = new TestCase();
        testCase.setId(33L);
        testCase.setProblem(problem);
        testCase.setCaseOrder(1);
        testCase.setInputData("");
        testCase.setExpectedOutput(expectedStdout());
        testCase.setWeight(new BigDecimal("1.00"));

        Submission submission = new Submission();
        submission.setId(44L);
        submission.setAssignment(assignment);
        submission.setStoragePath("submission-44");

        Batch batch = new Batch();
        batch.setId(55L);
        batch.setAssignment(assignment);

        ProblemRepository problemRepository = mock(ProblemRepository.class);
        when(problemRepository.findByAssignmentIdOrderByProblemOrderAsc(assignment.getId()))
                .thenReturn(List.of(problem));

        TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
        when(testCaseRepository.findByProblemIdOrderByCaseOrderAsc(problem.getId()))
                .thenReturn(List.of(testCase));

        List<SubmissionFile> submissionFiles = List.of(new SubmissionFile(
                "Main.java",
                submissionSource().getBytes(StandardCharsets.UTF_8)));

        SubmissionStorageService submissionStorageService = new FixedSubmissionStorageService(submissionFiles);
        GradingArtifactStorageService artifactStorageService = new TempWorkspaceArtifactStorageService(tempDir);

        AbstractSubmissionGrader grader = new TestSubmissionGrader(
                submissionStorageService,
                artifactStorageService,
                problemRepository,
                testCaseRepository,
                5000L);

        SubmissionGradingOutcome outcome = grader.grade(new GradingJob(assignment, batch, submission));

        assertThat(outcome.status()).isEqualTo("DONE");
        assertThat(outcome.testcasePassedCount()).isEqualTo(1);
        assertThat(outcome.testcaseTotalCount()).isEqualTo(1);
        assertThat(outcome.totalScore()).isEqualByComparingTo("10.00");
        assertThat(outcome.executionLog()).contains("Testcase 1: passed");
    }

    private static String submissionSource() {
        return """
                public class Main {
                    public static void main(String[] args) {
                        String stdoutLine = \"%s\";
                        String stderrLine = \"%s\";
                        for (int i = 0; i < %d; i++) {
                            System.out.println(stdoutLine);
                            System.err.println(stderrLine);
                        }
                    }
                }
                """.formatted(LARGE_LINE, STDERR_LINE, REPETITIONS);
    }

    private static String expectedStdout() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < REPETITIONS; i++) {
            output.append(LARGE_LINE).append('\n');
        }
        return output.toString();
    }

    private static final class TestSubmissionGrader extends AbstractSubmissionGrader {

        private TestSubmissionGrader(
                SubmissionStorageService submissionStorageService,
                GradingArtifactStorageService artifactStorageService,
                ProblemRepository problemRepository,
                TestCaseRepository testCaseRepository,
                long timeoutMillis) {
            super(submissionStorageService, artifactStorageService, problemRepository, testCaseRepository, timeoutMillis);
        }

        @Override
        protected List<RuleCheckOutcome> evaluateRuleOutcomes(GradingJob job, List<SubmissionFile> files) {
            return List.of();
        }

        @Override
        protected String resolveMainClass(List<SubmissionFile> files) {
            return "Main";
        }
    }

    private static final class TempWorkspaceArtifactStorageService implements GradingArtifactStorageService {

        private final Path root;

        private TempWorkspaceArtifactStorageService(Path root) {
            this.root = root;
        }

        @Override
        public PreparedWorkspace prepareWorkspace(Batch batch, Submission submission, List<SubmissionFile> files) {
            try {
                Path workspaceRoot = Files.createDirectories(root.resolve("workspace-" + submission.getId()));
                Path artifactRoot = Files.createDirectories(workspaceRoot.resolve("artifacts"));
                Path sourceRoot = Files.createDirectories(workspaceRoot.resolve("source"));
                Path classesRoot = Files.createDirectories(workspaceRoot.resolve("classes"));
                for (SubmissionFile file : files) {
                    Path target = sourceRoot.resolve(file.relativePath());
                    Files.createDirectories(target.getParent());
                    Files.write(target, file.content());
                }
                return new PreparedWorkspace(artifactRoot, sourceRoot, classesRoot);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not prepare temp grading workspace", ex);
            }
        }

        @Override
        public void writeTextArtifact(Path artifactRoot, String fileName, String content) {
            try {
                Files.writeString(artifactRoot.resolve(fileName), content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not write artifact", ex);
            }
        }

        @Override
        public void cleanupWorkspace(PreparedWorkspace workspace) {
            // Keep files until test end for easier failure inspection.
        }
    }

    private static final class FixedSubmissionStorageService implements SubmissionStorageService {

        private final List<SubmissionFile> files;

        private FixedSubmissionStorageService(List<SubmissionFile> files) {
            this.files = files;
        }

        @Override
        public String storeSubmissionFiles(
                Assignment assignment,
                Long submissionId,
                String submitterName,
                List<SubmissionFile> files) {
            throw new UnsupportedOperationException("storeSubmissionFiles is not used in this test");
        }

        @Override
        public List<SubmissionFile> loadSubmissionFiles(String storagePath) {
            return files;
        }

        @Override
        public void deleteSubmissionFiles(String storagePath) {
            throw new UnsupportedOperationException("deleteSubmissionFiles is not used in this test");
        }
    }
}
