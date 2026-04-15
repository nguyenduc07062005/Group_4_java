package com.group4.javagrader.grading.engine;

import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.storage.GradingArtifactStorageService;
import com.group4.javagrader.storage.SubmissionStorageService;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractSubmissionGrader implements SubmissionGrader {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^\\s*(?:public\\s+)?class\\s+([A-Za-z_]\\w*)");
    private static final Pattern MAIN_METHOD_PATTERN = Pattern.compile("public\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*]\\s*args\\s*\\)");
    private static final Pattern FENCED_CODE_BLOCK_PATTERN = Pattern.compile("(?s)^\\s*```[\\w-]*\\R?(.*?)\\R?```\\s*$");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("^\\s*`([^`\\r\\n]*)`\\s*$");

    private final SubmissionStorageService submissionStorageService;
    private final GradingArtifactStorageService artifactStorageService;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final long timeoutMillis;

    protected AbstractSubmissionGrader(
            SubmissionStorageService submissionStorageService,
            GradingArtifactStorageService artifactStorageService,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            long timeoutMillis) {
        this.submissionStorageService = submissionStorageService;
        this.artifactStorageService = artifactStorageService;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public final SubmissionGradingOutcome grade(GradingJob job) {
        List<SubmissionFile> files = submissionStorageService.loadSubmissionFiles(job.submission().getStoragePath());
        GradingArtifactStorageService.PreparedWorkspace workspace = artifactStorageService.prepareWorkspace(job.batch(), job.submission(), files);

        try {
            CompileOutcome compileOutcome = compile(workspace.sourceRoot(), workspace.classesRoot());
            artifactStorageService.writeTextArtifact(workspace.artifactRoot(), "compile.log", compileOutcome.log());
            if (!compileOutcome.success()) {
                return new SubmissionGradingOutcome(
                        "FAILED_COMPILE",
                        scale(BigDecimal.ZERO),
                        totalMaxScore(job),
                        scale(BigDecimal.ZERO),
                        scale(BigDecimal.ZERO),
                        0,
                        0,
                        compileOutcome.log(),
                        "Compilation failed before testcase execution started.",
                        null,
                        workspace.artifactRoot().toString(),
                        List.of(),
                        List.of());
            }

            List<RuleCheckOutcome> ruleOutcomes = evaluateRuleOutcomes(job, files);
            String mainClass = resolveMainClass(files);

            RuntimeEvaluation runtimeEvaluation = evaluateRuntime(job, workspace, mainClass);
            artifactStorageService.writeTextArtifact(workspace.artifactRoot(), "execution.log", runtimeEvaluation.executionLog());

            BigDecimal maxScore = totalMaxScore(job);
            BigDecimal logicContribution = weightLogicScore(runtimeEvaluation.rawLogicScore(), job);
            BigDecimal oopContribution = weightOopScore(ruleOutcomes, maxScore, job);
            BigDecimal totalScore = scale(logicContribution.add(oopContribution));

            return new SubmissionGradingOutcome(
                    runtimeEvaluation.terminalStatus(),
                    totalScore,
                    maxScore,
                    logicContribution,
                    oopContribution,
                    runtimeEvaluation.passedCount(),
                    runtimeEvaluation.totalCount(),
                    compileOutcome.log(),
                    runtimeEvaluation.executionLog(),
                    summarizeRuleViolations(ruleOutcomes),
                    workspace.artifactRoot().toString(),
                    runtimeEvaluation.problemOutcomes(),
                    ruleOutcomes);
        } finally {
            artifactStorageService.cleanupWorkspace(workspace);
        }
    }

    protected abstract List<RuleCheckOutcome> evaluateRuleOutcomes(GradingJob job, List<SubmissionFile> files);

    protected abstract String resolveMainClass(List<SubmissionFile> files);

    private CompileOutcome compile(Path sourceRoot, Path classesRoot) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompileOutcome(false, "Java compiler is not available in the current runtime.");
        }

        List<File> sourceFiles;
        try (var paths = Files.walk(sourceRoot)) {
            sourceFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.naturalOrder())
                    .map(Path::toFile)
                    .toList();
        } catch (IOException ex) {
            return new CompileOutcome(false, "Source files could not be scanned for compilation.");
        }

        if (sourceFiles.isEmpty()) {
            return new CompileOutcome(false, "Submission did not contain any Java source file.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            List<String> options = List.of("-d", classesRoot.toString());
            Boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();

            StringBuilder log = new StringBuilder();
            if (Boolean.TRUE.equals(success) && diagnostics.getDiagnostics().isEmpty()) {
                log.append("Compilation succeeded.");
            }
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                log.append(diagnostic.getKind())
                        .append(": line ")
                        .append(diagnostic.getLineNumber())
                        .append(" - ")
                        .append(diagnostic.getMessage(Locale.ROOT))
                        .append('\n');
            }
            return new CompileOutcome(Boolean.TRUE.equals(success), log.toString().strip());
        } catch (IOException ex) {
            return new CompileOutcome(false, "Compilation could not initialize the Java file manager.");
        }
    }

    private RuntimeEvaluation evaluateRuntime(
            GradingJob job,
            GradingArtifactStorageService.PreparedWorkspace workspace,
            String mainClass) {
        List<Problem> problems = problemRepository.findByAssignmentIdOrderByProblemOrderAsc(job.assignment().getId());
        List<ProblemOutcome> problemOutcomes = new ArrayList<>();
        StringBuilder executionLog = new StringBuilder();

        if (problems.isEmpty()) {
            executionLog.append("No problems were configured for this assignment.");
            return new RuntimeEvaluation("DONE", scale(BigDecimal.ZERO), 0, 0, executionLog.toString(), problemOutcomes);
        }

        if (mainClass == null || mainClass.isBlank()) {
            for (Problem problem : problems) {
                List<TestCase> testCases = testCaseRepository.findByProblemIdOrderByCaseOrderAsc(problem.getId());
                problemOutcomes.add(new ProblemOutcome(
                        problem,
                        "SKIPPED",
                        scale(BigDecimal.ZERO),
                        problem.getMaxScore(),
                        0,
                        testCases.size(),
                        "No runnable main entry point was found. Logic scoring was skipped.",
                        List.of()));
            }
            executionLog.append("No runnable main entry point was found. Testcase execution was skipped.");
            return new RuntimeEvaluation("DONE", scale(BigDecimal.ZERO), 0, totalTestCaseCount(problemOutcomes), executionLog.toString(), problemOutcomes);
        }

        BigDecimal rawLogicScore = scale(BigDecimal.ZERO);
        int passedCount = 0;
        int totalCount = 0;
        String terminalStatus = "DONE";

        for (Problem problem : problems) {
            List<TestCase> testCases = testCaseRepository.findByProblemIdOrderByCaseOrderAsc(problem.getId());
            if (testCases.isEmpty()) {
                problemOutcomes.add(new ProblemOutcome(
                        problem,
                        "NO_TESTCASES",
                        scale(BigDecimal.ZERO),
                        problem.getMaxScore(),
                        0,
                        0,
                        "Problem does not have any testcase yet.",
                        List.of()));
                continue;
            }

            BigDecimal totalWeight = testCases.stream()
                    .map(TestCase::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<TestCaseOutcome> testCaseOutcomes = new ArrayList<>();
            BigDecimal earnedScore = scale(BigDecimal.ZERO);
            int problemPassedCount = 0;
            boolean problemTerminalFailure = false;

            executionLog.append("Problem ").append(problem.getProblemOrder()).append(" - ").append(problem.getTitle()).append('\n');

            for (TestCase testCase : testCases) {
                totalCount++;
                ExecutionOutcome executionOutcome = runMainClass(
                        workspace.sourceRoot(),
                        workspace.classesRoot(),
                        mainClass,
                        problem.getInputMode(),
                        testCase.getInputData());

                BigDecimal testcaseMaxScore = scoreForTestCase(problem.getMaxScore(), totalWeight, testCase.getWeight());
                if (executionOutcome.timedOut()) {
                    testCaseOutcomes.add(new TestCaseOutcome(
                            testCase,
                            "FAILED_TIMEOUT",
                            false,
                            scale(BigDecimal.ZERO),
                            testCase.getWeight(),
                            normalizeLineEndings(testCase.getExpectedOutput()),
                            executionOutcome.stdout(),
                            "Execution exceeded the timeout of " + timeoutMillis + " ms.",
                            executionOutcome.runtimeMillis()));
                    executionLog.append(" - Testcase ").append(testCase.getCaseOrder()).append(": timeout").append('\n');
                    terminalStatus = "FAILED_TIMEOUT";
                    problemTerminalFailure = true;
                    break;
                }

                if (executionOutcome.exitCode() != 0) {
                    String failureMessage = executionOutcome.stderr().isBlank()
                            ? "Program exited with code " + executionOutcome.exitCode() + '.'
                            : executionOutcome.stderr();
                    testCaseOutcomes.add(new TestCaseOutcome(
                            testCase,
                            "FAILED_RUNTIME",
                            false,
                            scale(BigDecimal.ZERO),
                            testCase.getWeight(),
                            normalizeLineEndings(testCase.getExpectedOutput()),
                            executionOutcome.stdout(),
                            failureMessage,
                            executionOutcome.runtimeMillis()));
                    executionLog.append(" - Testcase ").append(testCase.getCaseOrder()).append(": runtime failure").append('\n');
                    terminalStatus = "FAILED_RUNTIME";
                    problemTerminalFailure = true;
                    break;
                }

                boolean passed = matchesExpectedOutput(job, problem, testCase, executionOutcome.stdout());
                BigDecimal testcaseScore = passed ? testcaseMaxScore : scale(BigDecimal.ZERO);
                if (passed) {
                    problemPassedCount++;
                    passedCount++;
                    earnedScore = scale(earnedScore.add(testcaseScore));
                }
                testCaseOutcomes.add(new TestCaseOutcome(
                        testCase,
                        passed ? "PASSED" : "FAILED_OUTPUT",
                        passed,
                        testcaseScore,
                        testCase.getWeight(),
                        normalizeLineEndings(testCase.getExpectedOutput()),
                        normalizeLineEndings(executionOutcome.stdout()),
                        passed ? "Output matched expected result." : "Output did not match expected result.",
                        executionOutcome.runtimeMillis()));
                executionLog.append(" - Testcase ").append(testCase.getCaseOrder())
                        .append(": ").append(passed ? "passed" : "failed output comparison")
                        .append('\n');
            }

            rawLogicScore = scale(rawLogicScore.add(earnedScore));
            problemOutcomes.add(new ProblemOutcome(
                    problem,
                    problemTerminalFailure ? terminalStatus : "DONE",
                    earnedScore,
                    problem.getMaxScore(),
                    problemPassedCount,
                    testCases.size(),
                    problemTerminalFailure
                            ? "Execution stopped because of " + terminalStatus + '.'
                            : "Completed " + problemPassedCount + "/" + testCases.size() + " testcase(s).",
                    testCaseOutcomes));

            if (problemTerminalFailure) {
                break;
            }
        }

        return new RuntimeEvaluation(terminalStatus, rawLogicScore, passedCount, totalCount, executionLog.toString().strip(), problemOutcomes);
    }

    private ExecutionOutcome runMainClass(
            Path workingDirectory,
            Path classesRoot,
            String mainClass,
            InputMode inputMode,
            String inputData) {
        Path inputFile = workingDirectory.resolve("input.txt");
        String preparedInput = sanitizeFixtureText(inputData);
        try {
            if (inputMode == InputMode.FILE) {
                Files.writeString(inputFile, preparedInput, StandardCharsets.UTF_8);
            } else {
                Files.deleteIfExists(inputFile);
            }
        } catch (IOException ex) {
            return new ExecutionOutcome("", "Input fixture could not be prepared.", -1, false, 0L);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                resolveJavaExecutable(),
                "-cp",
                classesRoot.toString(),
                mainClass);
        processBuilder.directory(workingDirectory.toFile());

        long startedAt = System.nanoTime();
        ExecutorService streamReaders = Executors.newFixedThreadPool(2);
        try {
            Process process = processBuilder.start();
            Future<String> stdoutFuture = streamReaders.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = streamReaders.submit(() -> readStream(process.getErrorStream()));
            if (inputMode != InputMode.FILE) {
                try (var outputStream = process.getOutputStream()) {
                    outputStream.write(preparedInput.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                process.getOutputStream().close();
            }

            boolean finished = process.waitFor(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
            long runtimeMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
                return new ExecutionOutcome(awaitStream(stdoutFuture), awaitStream(stderrFuture), -1, true, runtimeMillis);
            }

            return new ExecutionOutcome(
                    awaitStream(stdoutFuture),
                    awaitStream(stderrFuture),
                    process.exitValue(),
                    false,
                    runtimeMillis);
        } catch (IOException ex) {
            return new ExecutionOutcome("", "Process execution could not start.", -1, false, 0L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ExecutionOutcome("", "Execution thread was interrupted.", -1, false, 0L);
        } finally {
            streamReaders.shutdownNow();
        }
    }

    private String resolveJavaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private String readStream(java.io.InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private String awaitStream(Future<String> streamFuture) throws InterruptedException {
        try {
            return streamFuture.get();
        } catch (ExecutionException ex) {
            return "";
        }
    }

    private BigDecimal weightLogicScore(BigDecimal rawLogicScore, GradingJob job) {
        return scale(rawLogicScore
                .multiply(BigDecimal.valueOf(job.assignment().getLogicWeight()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal weightOopScore(List<RuleCheckOutcome> ruleOutcomes, BigDecimal maxScore, GradingJob job) {
        if (job.assignment().getOopWeight() == null || job.assignment().getOopWeight() == 0) {
            return scale(BigDecimal.ZERO);
        }

        if (ruleOutcomes.isEmpty()) {
            return scale(BigDecimal.ZERO);
        }

        long passedRules = ruleOutcomes.stream().filter(RuleCheckOutcome::passed).count();
        BigDecimal compliance = BigDecimal.valueOf(passedRules)
                .divide(BigDecimal.valueOf(ruleOutcomes.size()), 4, RoundingMode.HALF_UP);
        return scale(maxScore
                .multiply(compliance)
                .multiply(BigDecimal.valueOf(job.assignment().getOopWeight()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal totalMaxScore(GradingJob job) {
        return scale(problemRepository.findByAssignmentIdOrderByProblemOrderAsc(job.assignment().getId()).stream()
                .map(Problem::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private int totalTestCaseCount(List<ProblemOutcome> problemOutcomes) {
        return problemOutcomes.stream().mapToInt(ProblemOutcome::testcaseTotalCount).sum();
    }

    private BigDecimal scoreForTestCase(BigDecimal problemMaxScore, BigDecimal totalWeight, BigDecimal caseWeight) {
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return scale(BigDecimal.ZERO);
        }
        return scale(problemMaxScore
                .multiply(caseWeight)
                .divide(totalWeight, 2, RoundingMode.HALF_UP));
    }

    private boolean matchesExpectedOutput(GradingJob job, Problem problem, TestCase testCase, String actualOutput) {
        OutputComparisonMode effectiveMode = effectiveComparisonMode(
                job.assignment().getOutputNormalizationPolicy(),
                problem.getOutputComparisonMode());
        String expected = normalizeForComparison(sanitizeFixtureText(testCase.getExpectedOutput()), effectiveMode);
        String actual = normalizeForComparison(actualOutput, effectiveMode);
        return expected.equals(actual);
    }

    private OutputComparisonMode effectiveComparisonMode(
            OutputNormalizationPolicy assignmentPolicy,
            OutputComparisonMode problemMode) {
        OutputComparisonMode assignmentMode = OutputComparisonMode.fromAssignmentPolicy(assignmentPolicy);
        OutputComparisonMode resolvedProblemMode = problemMode == null ? OutputComparisonMode.EXACT : problemMode;
        return assignmentMode.getNormalizationRank() >= resolvedProblemMode.getNormalizationRank()
                ? assignmentMode
                : resolvedProblemMode;
    }

    private String normalizeForComparison(String output, OutputComparisonMode mode) {
        String normalized = normalizeLineEndings(output);
        if (mode == OutputComparisonMode.IGNORE_WHITESPACE) {
            return normalized.replaceAll("\\s+", "");
        }
        if (mode == OutputComparisonMode.TRIM_ALL) {
            String[] lines = normalized.split("\n", -1);
            List<String> trimmedLines = new ArrayList<>();
            for (String line : lines) {
                trimmedLines.add(line.trim());
            }
            return String.join("\n", trimmedLines).trim();
        }
        return stripTerminalLineBreaks(normalized);
    }

    protected String normalizeLineEndings(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    protected String sanitizeFixtureText(String value) {
        String normalized = normalizeLineEndings(defaultString(value));

        Matcher fencedBlockMatcher = FENCED_CODE_BLOCK_PATTERN.matcher(normalized);
        if (fencedBlockMatcher.matches()) {
            return fencedBlockMatcher.group(1);
        }

        Matcher inlineCodeMatcher = INLINE_CODE_PATTERN.matcher(normalized);
        if (inlineCodeMatcher.matches()) {
            return inlineCodeMatcher.group(1);
        }

        return normalized;
    }

    protected String stripTerminalLineBreaks(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '\n') {
            end--;
        }
        return value.substring(0, end);
    }

    protected String determinePackageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    protected String determineClassName(String source) {
        Matcher matcher = CLASS_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    protected boolean containsMainMethod(String source) {
        return MAIN_METHOD_PATTERN.matcher(source).find();
    }

    protected BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    protected String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String summarizeRuleViolations(List<RuleCheckOutcome> ruleOutcomes) {
        if (ruleOutcomes.isEmpty()) {
            return null;
        }

        List<String> failedRules = ruleOutcomes.stream()
                .filter(rule -> !rule.passed())
                .map(RuleCheckOutcome::ruleLabel)
                .toList();
        if (failedRules.isEmpty()) {
            return "No OOP rule violations.";
        }
        return String.join(", ", failedRules);
    }

    private record CompileOutcome(boolean success, String log) {
    }

    private record ExecutionOutcome(
            String stdout,
            String stderr,
            int exitCode,
            boolean timedOut,
            long runtimeMillis) {
    }

    private record RuntimeEvaluation(
            String terminalStatus,
            BigDecimal rawLogicScore,
            int passedCount,
            int totalCount,
            String executionLog,
            List<ProblemOutcome> problemOutcomes) {
    }
}
