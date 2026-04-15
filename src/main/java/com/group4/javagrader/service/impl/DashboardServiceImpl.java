package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AssignmentGroupView;
import com.group4.javagrader.dto.CourseBoardView;
import com.group4.javagrader.dto.DashboardView;
import com.group4.javagrader.dto.SemesterBoardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.PlagiarismReportRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.service.DashboardService;
import com.group4.javagrader.service.TeacherBoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TeacherBoardService teacherBoardService;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    private final SubmissionRepository submissionRepository;
    private final PlagiarismReportRepository plagiarismReportRepository;
    private final BatchRepository batchRepository;
    private final GradingResultRepository gradingResultRepository;

    public DashboardServiceImpl(
            TeacherBoardService teacherBoardService,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            SubmissionRepository submissionRepository,
            PlagiarismReportRepository plagiarismReportRepository,
            BatchRepository batchRepository,
            GradingResultRepository gradingResultRepository) {
        this.teacherBoardService = teacherBoardService;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.plagiarismReportRepository = plagiarismReportRepository;
        this.batchRepository = batchRepository;
        this.gradingResultRepository = gradingResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView build() {
        List<SemesterBoardView> semesterBoards = teacherBoardService.buildActiveSemesterBoard();
        Map<Long, AssignmentInsight> insightsByAssignmentId = buildInsights(semesterBoards);

        List<DashboardView.SemesterSection> semesters = semesterBoards.stream()
                .map(semesterBoard -> toSemesterSection(semesterBoard, insightsByAssignmentId))
                .toList();

        List<AssignmentInsight> insights = new ArrayList<>(insightsByAssignmentId.values());
        insights.sort(Comparator.comparing(AssignmentInsight::touchedAt, this::compareTimes).reversed());

        return new DashboardView(
                buildStats(semesterBoards, insights),
                semesters,
                semesters.isEmpty() ? null : semesters.get(0).getId(),
                buildAttentionItems(insights),
                buildRunningBatches(insights),
                buildRecentActivities(insights),
                buildTaskRows(insights),
                buildCourseStatusBars(semesters, insightsByAssignmentId),
                buildWeekLoadBars(insights));
    }

    private Map<Long, AssignmentInsight> buildInsights(List<SemesterBoardView> semesterBoards) {
        List<Assignment> assignments = new ArrayList<>();
        Map<Long, AssignmentGroupView> groupsByAssignmentId = new LinkedHashMap<>();
        for (SemesterBoardView semesterBoard : semesterBoards) {
            for (CourseBoardView courseBoard : semesterBoard.getCourses()) {
                for (AssignmentGroupView group : courseBoard.getAssignmentGroups()) {
                    for (Assignment assignment : group.getAssignments()) {
                        assignments.add(assignment);
                        groupsByAssignmentId.put(assignment.getId(), group);
                    }
                }
            }
        }

        if (assignments.isEmpty()) {
            return Map.of();
        }

        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .toList();
        Map<Long, List<Problem>> questionsByAssignmentId = groupQuestionsByAssignment(assignmentIds);
        List<Long> problemIds = questionsByAssignmentId.values().stream()
                .flatMap(List::stream)
                .map(Problem::getId)
                .toList();
        Map<Long, Long> testcaseCountByProblemId = summarizeTestcases(problemIds);
        Map<Long, SubmissionSummary> submissionSummariesByAssignmentId = summarizeSubmissions(assignmentIds);
        Map<Long, PlagiarismReport> latestReportsByAssignmentId = indexLatestReports(assignmentIds);
        Map<Long, Batch> latestBatchesByAssignmentId = indexLatestBatches(assignmentIds);
        Map<Long, BatchProgressSummary> batchProgressByBatchId = summarizeBatchProgress(latestBatchesByAssignmentId.values());

        Map<Long, AssignmentInsight> insightsByAssignmentId = new LinkedHashMap<>();
        for (Assignment assignment : assignments) {
            Long assignmentId = assignment.getId();
            Batch latestBatch = latestBatchesByAssignmentId.get(assignmentId);
            insightsByAssignmentId.put(assignmentId, buildInsight(
                    assignment,
                    groupsByAssignmentId.get(assignmentId),
                    questionsByAssignmentId.getOrDefault(assignmentId, List.of()),
                    testcaseCountByProblemId,
                    submissionSummariesByAssignmentId.get(assignmentId),
                    latestReportsByAssignmentId.get(assignmentId),
                    latestBatch,
                    latestBatch != null ? batchProgressByBatchId.get(latestBatch.getId()) : null));
        }
        return insightsByAssignmentId;
    }

    private AssignmentInsight buildInsight(
            Assignment assignment,
            AssignmentGroupView group,
            List<Problem> questions,
            Map<Long, Long> testcaseCountByProblemId,
            SubmissionSummary submissionSummary,
            PlagiarismReport latestReport,
            Batch latestBatch,
            BatchProgressSummary progress) {
        long testcaseCount = 0;
        int configuredQuestionCount = 0;
        for (Problem question : questions) {
            long questionTestcaseCount = testcaseCountByProblemId.getOrDefault(question.getId(), 0L);
            testcaseCount += questionTestcaseCount;
            if (questionTestcaseCount > 0) {
                configuredQuestionCount++;
            }
        }

        boolean setupReady = !questions.isEmpty() && configuredQuestionCount == questions.size();

        int submissionCount = submissionSummary != null ? submissionSummary.submissionCount() : 0;
        int validatedCount = submissionSummary != null ? submissionSummary.validatedCount() : 0;
        int rejectedCount = submissionSummary != null ? submissionSummary.rejectedCount() : 0;
        LocalDateTime latestSubmissionAt = submissionSummary != null ? submissionSummary.latestSubmissionAt() : null;

        StatusDescriptor status = classifyStatus(
                assignment,
                questions.size(),
                setupReady,
                submissionCount,
                validatedCount,
                rejectedCount,
                latestSubmissionAt,
                latestReport,
                latestBatch);

        ActivitySeed activity = buildActivitySeed(assignment, latestSubmissionAt, latestReport, latestBatch);
        LocalDateTime touchedAt = latestOf(
                assignment.getUpdatedAt(),
                assignment.getCreatedAt(),
                latestSubmissionAt,
                reportTimestamp(latestReport),
                batchTimestamp(latestBatch));

        return new AssignmentInsight(
                assignment,
                group != null ? group.getLabel() : "Assignments",
                questions.size(),
                testcaseCount,
                submissionCount,
                validatedCount,
                status.blockedCount(),
                latestBatch,
                progress,
                status,
                activity,
                touchedAt);
    }

    private DashboardView.SemesterSection toSemesterSection(
            SemesterBoardView semesterBoard,
            Map<Long, AssignmentInsight> insightsByAssignmentId) {
        List<DashboardView.CourseSection> courses = semesterBoard.getCourses().stream()
                .map(courseBoard -> toCourseSection(semesterBoard, courseBoard, insightsByAssignmentId))
                .toList();

        Long semesterId = semesterBoard.getSemester().getId();
        return new DashboardView.SemesterSection(
                semesterId,
                semesterBoard.getSemester().getCode(),
                semesterBoard.getSemester().getName(),
                formatSchedule(semesterBoard.getSemester().getStartDate(), semesterBoard.getSemester().getEndDate()),
                semesterBoard.getCourses().size(),
                semesterBoard.getAssignmentCount(),
                "/semesters/" + semesterId,
                "/courses/create?semesterId=" + semesterId,
                buildSemesterAssignmentUrl(semesterBoard),
                courses);
    }

    private DashboardView.CourseSection toCourseSection(
            SemesterBoardView semesterBoard,
            CourseBoardView courseBoard,
            Map<Long, AssignmentInsight> insightsByAssignmentId) {
        List<DashboardView.AssignmentLane> lanes = courseBoard.getAssignmentGroups().stream()
                .map(group -> new DashboardView.AssignmentLane(
                        group.getLabel(),
                        group.getHelper(),
                        group.getIcon(),
                        group.getAssignments().stream()
                                .map(assignment -> toAssignmentCard(insightsByAssignmentId.get(assignment.getId())))
                                .toList()))
                .toList();

        return new DashboardView.CourseSection(
                courseBoard.getCourse().getId(),
                courseBoard.getCourse().getCourseCode(),
                courseBoard.getCourse().getCourseName(),
                courseBoard.getCourse().getWeekCount(),
                courseBoard.getAssignmentCount(),
                "/assignments/create?semesterId=" + semesterBoard.getSemester().getId()
                        + "&courseId=" + courseBoard.getCourse().getId(),
                lanes);
    }

    private DashboardView.AssignmentCard toAssignmentCard(AssignmentInsight insight) {
        return new DashboardView.AssignmentCard(
                insight.assignment().getId(),
                insight.assignment().getAssignmentName(),
                insight.assignment().getGradingMode(),
                insight.status().stageKey(),
                insight.status().stageLabel(),
                insight.status().statusLabel(),
                insight.status().statusHelper(),
                insight.questionCount(),
                insight.testcaseCount(),
                insight.submissionCount(),
                insight.validatedCount(),
                insight.status().actionLabel(),
                insight.status().actionUrl());
    }

    private List<DashboardView.StatCard> buildStats(
            List<SemesterBoardView> semesterBoards,
            List<AssignmentInsight> insights) {
        int courseCount = semesterBoards.stream()
                .mapToInt(semesterBoard -> semesterBoard.getCourses().size())
                .sum();
        int assignmentCount = insights.size();
        long readyForIntake = insights.stream()
                .filter(insight -> "intake".equals(insight.status().stageKey()) && insight.submissionCount() == 0)
                .count();
        int validatedSubmissions = insights.stream()
                .mapToInt(AssignmentInsight::validatedCount)
                .sum();
        int blockedCases = insights.stream()
                .mapToInt(AssignmentInsight::blockedCount)
                .sum();
        long runningBatches = insights.stream()
                .filter(insight -> insight.latestBatch() != null && isActiveBatch(insight.latestBatch()))
                .count();

        return List.of(
                new DashboardView.StatCard("Courses", String.valueOf(courseCount), "Active teaching spaces in the current workspace.", "school"),
                new DashboardView.StatCard("Assignments", String.valueOf(assignmentCount), "Weekly and custom grading workspaces currently available.", "assignment"),
                new DashboardView.StatCard("Ready for intake", String.valueOf(readyForIntake), "Setup is complete and ZIP uploads can start immediately.", "upload_file"),
                new DashboardView.StatCard("Validated files", String.valueOf(validatedSubmissions), "Submission folders that passed upload validation.", "fact_check"),
                new DashboardView.StatCard("Plagiarism blocked", String.valueOf(blockedCases), "Submissions blocked by the latest plagiarism decision.", "policy_alert"),
                new DashboardView.StatCard("Running batches", String.valueOf(runningBatches), "Frozen grading runs still in queue or execution.", "manufacturing"));
    }

    private List<DashboardView.AttentionItem> buildAttentionItems(List<AssignmentInsight> insights) {
        return insights.stream()
                .filter(insight -> insight.status().attention())
                .sorted(Comparator
                        .comparingInt((AssignmentInsight insight) -> insight.status().priority())
                        .reversed()
                        .thenComparing(AssignmentInsight::touchedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(insight -> new DashboardView.AttentionItem(
                        attentionIcon(insight.status().stageKey()),
                        insight.assignment().getAssignmentName(),
                        insight.status().statusHelper(),
                        courseLabel(insight.assignment()),
                        insight.status().actionLabel(),
                        insight.status().actionUrl()))
                .toList();
    }

    private List<DashboardView.RunningBatchItem> buildRunningBatches(List<AssignmentInsight> insights) {
        return insights.stream()
                .filter(insight -> insight.latestBatch() != null && isActiveBatch(insight.latestBatch()))
                .sorted(Comparator.comparing(AssignmentInsight::touchedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(5)
                .map(this::toRunningBatchItem)
                .toList();
    }

    private DashboardView.RunningBatchItem toRunningBatchItem(AssignmentInsight insight) {
        BatchProgressSummary progress = insight.progress();
        int progressPercent = progress == null || progress.totalCount() == 0
                ? 0
                : (int) Math.round((progress.completedCount() * 100.0) / progress.totalCount());
        String progressLabel = progress == null
                ? "Queued for grading."
                : progress.completedCount() + " / " + progress.totalCount() + " submissions completed";

        return new DashboardView.RunningBatchItem(
                insight.assignment().getAssignmentName(),
                courseLabel(insight.assignment()),
                humanizeBatchStatus(insight.latestBatch().getStatus()),
                progressLabel,
                progressPercent,
                "Open grading",
                "/assignments/" + insight.assignment().getId() + "/grading#batch-progress");
    }

    private List<DashboardView.ActivityItem> buildRecentActivities(List<AssignmentInsight> insights) {
        return insights.stream()
                .map(AssignmentInsight::activity)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(ActivitySeed::timestamp, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(6)
                .map(activity -> new DashboardView.ActivityItem(
                        activity.icon(),
                        activity.title(),
                        activity.detail(),
                        formatDateTime(activity.timestamp()),
                        activity.actionLabel(),
                        activity.actionUrl()))
                .toList();
    }

    private List<DashboardView.TaskRow> buildTaskRows(List<AssignmentInsight> insights) {
        return insights.stream()
                .filter(insight -> insight.status().showInTaskTable())
                .sorted(Comparator
                        .comparingInt((AssignmentInsight insight) -> insight.status().priority())
                        .reversed()
                        .thenComparing(AssignmentInsight::touchedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(insight -> new DashboardView.TaskRow(
                        insight.assignment().getAssignmentName(),
                        courseLabel(insight.assignment()),
                        insight.groupLabel(),
                        insight.status().stageLabel(),
                        insight.status().statusLabel(),
                        insight.validatedCount() + " validated / " + insight.submissionCount() + " total",
                        formatDateTime(insight.touchedAt()),
                        insight.status().actionLabel(),
                        insight.status().actionUrl()))
                .toList();
    }

    private List<DashboardView.CourseStatusBar> buildCourseStatusBars(
            List<DashboardView.SemesterSection> semesters,
            Map<Long, AssignmentInsight> insightsByAssignmentId) {
        List<DashboardView.CourseStatusBar> bars = new ArrayList<>();
        for (DashboardView.SemesterSection semester : semesters) {
            for (DashboardView.CourseSection course : semester.getCourses()) {
                toCourseStatusBar(course, insightsByAssignmentId).ifPresent(bars::add);
            }
        }

        return bars.stream()
                .sorted(Comparator.comparing(DashboardView.CourseStatusBar::getCourseLabel))
                .limit(8)
                .toList();
    }

    private Optional<DashboardView.CourseStatusBar> toCourseStatusBar(
            DashboardView.CourseSection course,
            Map<Long, AssignmentInsight> insightsByAssignmentId) {
        StageCounts stageCounts = countStageTotals(course, insightsByAssignmentId);
        if (stageCounts.totalCount() == 0) {
            return Optional.empty();
        }

        return Optional.of(new DashboardView.CourseStatusBar(
                course.getCode() + " - " + course.getName(),
                stageCounts.setupCount(),
                stageCounts.intakeCount(),
                stageCounts.plagiarismCount(),
                stageCounts.batchCount(),
                stageCounts.resultsCount(),
                stageCounts.totalCount()));
    }

    private StageCounts countStageTotals(
            DashboardView.CourseSection course,
            Map<Long, AssignmentInsight> insightsByAssignmentId) {
        int setupCount = 0;
        int intakeCount = 0;
        int plagiarismCount = 0;
        int batchCount = 0;
        int resultsCount = 0;
        int totalCount = 0;

        for (DashboardView.AssignmentLane lane : course.getLanes()) {
            for (DashboardView.AssignmentCard assignmentCard : lane.getAssignments()) {
                AssignmentInsight insight = insightsByAssignmentId.get(assignmentCard.getId());
                if (insight == null) {
                    continue;
                }
                totalCount++;
                switch (insight.status().stageKey()) {
                    case "setup" -> setupCount++;
                    case "intake" -> intakeCount++;
                    case "plagiarism" -> plagiarismCount++;
                    case "batch" -> batchCount++;
                    case "results" -> resultsCount++;
                    default -> {
                    }
                }
            }
        }

        return new StageCounts(setupCount, intakeCount, plagiarismCount, batchCount, resultsCount, totalCount);
    }

    private List<DashboardView.WeekLoadBar> buildWeekLoadBars(List<AssignmentInsight> insights) {
        Map<Integer, Integer> submissionsByWeek = insights.stream()
                .filter(insight -> insight.assignment().getWeekNumber() != null)
                .collect(Collectors.groupingBy(
                        insight -> insight.assignment().getWeekNumber(),
                        LinkedHashMap::new,
                        Collectors.summingInt(AssignmentInsight::submissionCount)));

        int max = submissionsByWeek.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return submissionsByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(10)
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new DashboardView.WeekLoadBar(
                        "Week " + entry.getKey(),
                        entry.getValue(),
                        max == 0 ? 0 : (int) Math.round((entry.getValue() * 100.0) / max)))
                .toList();
    }

    private Map<Long, List<Problem>> groupQuestionsByAssignment(List<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        return problemRepository.findByAssignmentIdInAndInternalDefaultFalseOrderByAssignmentIdAscProblemOrderAsc(assignmentIds).stream()
                .collect(Collectors.groupingBy(
                        problem -> problem.getAssignment().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<Long, Long> summarizeTestcases(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }

        return testCaseRepository.summarizeByProblemIds(problemIds).stream()
                .collect(Collectors.toMap(
                        TestCaseRepository.ProblemTestCaseCount::getProblemId,
                        TestCaseRepository.ProblemTestCaseCount::getTestcaseCount,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, SubmissionSummary> summarizeSubmissions(List<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        return submissionRepository.summarizeByAssignmentIds(assignmentIds).stream()
                .collect(Collectors.toMap(
                        SubmissionRepository.AssignmentSubmissionStats::getAssignmentId,
                        stats -> new SubmissionSummary(
                                Math.toIntExact(stats.getSubmissionCount()),
                                Math.toIntExact(stats.getValidatedCount()),
                                Math.toIntExact(stats.getRejectedCount()),
                                stats.getLatestSubmissionAt()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, PlagiarismReport> indexLatestReports(List<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, PlagiarismReport> latestReportsByAssignmentId = new LinkedHashMap<>();
        for (PlagiarismReport report : plagiarismReportRepository.findByAssignmentIdInOrderByAssignmentIdAscStartedAtDescIdDesc(assignmentIds)) {
            latestReportsByAssignmentId.putIfAbsent(report.getAssignment().getId(), report);
        }
        return latestReportsByAssignmentId;
    }

    private Map<Long, Batch> indexLatestBatches(List<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Batch> latestBatchesByAssignmentId = new LinkedHashMap<>();
        for (Batch batch : batchRepository.findByAssignmentIdInOrderByAssignmentIdAscIdDesc(assignmentIds)) {
            latestBatchesByAssignmentId.putIfAbsent(batch.getAssignment().getId(), batch);
        }
        return latestBatchesByAssignmentId;
    }

    private Map<Long, BatchProgressSummary> summarizeBatchProgress(Iterable<Batch> latestBatches) {
        List<Long> activeBatchIds = new ArrayList<>();
        for (Batch batch : latestBatches) {
            if (batch != null && isActiveBatch(batch)) {
                activeBatchIds.add(batch.getId());
            }
        }

        if (activeBatchIds.isEmpty()) {
            return Map.of();
        }

        return gradingResultRepository.summarizeByBatchIds(activeBatchIds).stream()
                .collect(Collectors.toMap(
                        GradingResultRepository.BatchProgressStats::getBatchId,
                        stats -> new BatchProgressSummary(stats.getTotalCount(), stats.getCompletedCount()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private StatusDescriptor classifyStatus(
            Assignment assignment,
            int questionCount,
            boolean setupReady,
            int submissionCount,
            int validatedCount,
            int rejectedCount,
            LocalDateTime latestSubmissionAt,
            PlagiarismReport latestReport,
            Batch latestBatch) {
        return DashboardStatusClassifier.classify(
                assignment,
                questionCount,
                setupReady,
                submissionCount,
                validatedCount,
                rejectedCount,
                latestSubmissionAt,
                latestReport,
                latestBatch);
    }

    static final class DashboardStatusClassifier {

        private DashboardStatusClassifier() {
        }

        static StatusDescriptor classify(
                Assignment assignment,
                int questionCount,
                boolean setupReady,
                int submissionCount,
                int validatedCount,
                int rejectedCount,
                LocalDateTime latestSubmissionAt,
                PlagiarismReport latestReport,
                Batch latestBatch) {
            Long assignmentId = assignment.getId();
            String workspaceUrl = "/assignments/" + assignmentId;
            String uploadUrl = "/assignments/" + assignmentId + "/submissions/upload";
            String plagiarismUrl = "/assignments/" + assignmentId + "/plagiarism";
            String gradingUrl = "/assignments/" + assignmentId + "/grading";

            if (!setupReady) {
                String label = questionCount == 0 ? "Create the first question" : "Finish testcase coverage";
                String helper = questionCount == 0
                        ? "This assignment has no teacher-authored question yet."
                        : "Every question needs at least one testcase before intake can begin.";
                return new StatusDescriptor("setup", "Setup", label, helper, "Open workspace", workspaceUrl, 5, true, true, 0);
            }

            if (submissionCount == 0) {
                return new StatusDescriptor(
                        "intake",
                        "Intake",
                        "Ready for ZIP upload",
                        "Question and testcase setup is complete. Upload student folders next.",
                        "Upload files",
                        uploadUrl,
                        2,
                        true,
                        true,
                        0);
            }

            if (validatedCount == 0) {
                return new StatusDescriptor(
                        "intake",
                        "Intake",
                        "Validation blocked all uploads",
                        rejectedCount > 0
                                ? rejectedCount + " submission(s) were rejected during intake validation."
                                : "No validated submission is available yet.",
                        "Review intake",
                        uploadUrl,
                        4,
                        true,
                        true,
                        0);
            }

            if (latestBatch != null && isTerminalBatch(latestBatch)) {
                String label = latestBatch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS
                        ? "Results ready with grading errors"
                        : "Results ready";
                String helper = "Latest batch finished. Open the result table for detail.";
                return new StatusDescriptor("results", "Results", label, helper, "Open grading", gradingUrl + "#results-overview", 0, false, false, 0);
            }

            if (latestBatch != null && isActiveBatch(latestBatch)) {
                return new StatusDescriptor(
                        "batch",
                        "Batch",
                        "Batch is running",
                        "The frozen grading snapshot is queued or currently executing.",
                        "Open grading",
                        gradingUrl + "#batch-progress",
                        1,
                        false,
                        true,
                        0);
            }

            if (latestBatch != null && latestBatch.canStart()) {
                return new StatusDescriptor(
                        "batch",
                        "Batch",
                        "Snapshot is ready to start",
                        "Precheck already prepared a frozen grading batch.",
                        "Open grading",
                        gradingUrl + "#snapshot-run",
                        3,
                        true,
                        true,
                        latestReport != null ? latestReport.getBlockedSubmissionCount() : 0);
            }

            boolean reportMissing = latestReport == null || !latestReport.isCompleted();
            boolean reportStale = latestReport != null
                    && latestReport.getCompletedAt() != null
                    && latestSubmissionAt != null
                    && latestSubmissionAt.isAfter(latestReport.getCompletedAt());
            int blockedCount = latestReport != null ? latestReport.getBlockedSubmissionCount() : 0;

            if (validatedCount >= 2 && (reportMissing || reportStale || blockedCount > 0)) {
                String label;
                String helper;
                if (reportMissing) {
                    label = "Run plagiarism before batch";
                    helper = "At least two validated submissions are ready for plagiarism review.";
                } else if (reportStale) {
                    label = "Plagiarism report is outdated";
                    helper = "New uploads arrived after the last report. Re-run plagiarism before grading.";
                } else {
                    label = blockedCount + " blocked submission(s)";
                    helper = "Manual review is needed before freezing the batch.";
                }

                return new StatusDescriptor(
                        "plagiarism",
                        "Plagiarism",
                        label,
                        helper,
                        "Review check",
                        plagiarismUrl,
                        blockedCount > 0 ? 5 : 3,
                        true,
                        true,
                        blockedCount);
            }

            if (validatedCount >= 2) {
                return new StatusDescriptor(
                        "batch",
                        "Batch",
                        "Ready for grading",
                        "Validated uploads and plagiarism review are complete.",
                        "Open grading",
                        gradingUrl + "#snapshot-run",
                        2,
                        false,
                        true,
                        latestReport != null ? latestReport.getBlockedSubmissionCount() : 0);
            }

            return new StatusDescriptor(
                    "intake",
                    "Intake",
                    "Need more validated uploads",
                    "At least two validated submissions are recommended before plagiarism and batch.",
                    "Upload files",
                    uploadUrl,
                    2,
                    true,
                    true,
                    latestReport != null ? latestReport.getBlockedSubmissionCount() : 0);
        }

        private static boolean isActiveBatch(Batch batch) {
            return batch.getStatus() == BatchStatus.QUEUED
                    || batch.getStatus() == BatchStatus.READY_FOR_GRADING
                    || batch.getStatus() == BatchStatus.RUNNING;
        }

        private static boolean isTerminalBatch(Batch batch) {
            return batch.getStatus() == BatchStatus.COMPLETED
                    || batch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS;
        }
    }

    private ActivitySeed buildActivitySeed(
            Assignment assignment,
            LocalDateTime latestSubmissionAt,
            PlagiarismReport latestReport,
            Batch latestBatch) {
        LocalDateTime batchTime = batchTimestamp(latestBatch);
        if (batchTime != null) {
            return new ActivitySeed(
                    batchTime,
                    "manufacturing",
                    "Batch " + humanizeBatchStatus(latestBatch.getStatus()),
                    assignment.getAssignmentName() + " in " + courseLabel(assignment),
                    "Open grading",
                    "/assignments/" + assignment.getId() + "/grading#batch-progress");
        }

        LocalDateTime reportTime = reportTimestamp(latestReport);
        if (reportTime != null) {
            return new ActivitySeed(
                    reportTime,
                    "policy_alert",
                    "Plagiarism report updated",
                    assignment.getAssignmentName() + " now has " + latestReport.getBlockedSubmissionCount() + " blocked submission(s).",
                    "Review check",
                    "/assignments/" + assignment.getId() + "/plagiarism");
        }

        if (latestSubmissionAt != null) {
            return new ActivitySeed(
                    latestSubmissionAt,
                    "upload_file",
                    "Submission intake updated",
                    "New ZIP content arrived for " + assignment.getAssignmentName() + ".",
                    "Review intake",
                    "/assignments/" + assignment.getId() + "/submissions/upload");
        }

        LocalDateTime assignmentTime = latestOf(assignment.getUpdatedAt(), assignment.getCreatedAt());
        if (assignmentTime != null) {
            return new ActivitySeed(
                    assignmentTime,
                    "edit_square",
                    "Assignment workspace edited",
                    assignment.getAssignmentName() + " was updated in " + courseLabel(assignment) + ".",
                    "Open workspace",
                    "/assignments/" + assignment.getId());
        }

        return null;
    }

    private String buildSemesterAssignmentUrl(SemesterBoardView semesterBoard) {
        Optional<Long> firstCourseId = semesterBoard.getCourses().stream()
                .map(courseBoard -> courseBoard.getCourse().getId())
                .findFirst();
        if (firstCourseId.isPresent()) {
            return "/assignments/create?semesterId=" + semesterBoard.getSemester().getId()
                    + "&courseId=" + firstCourseId.get();
        }
        return "/courses/create?semesterId=" + semesterBoard.getSemester().getId();
    }

    private String courseLabel(Assignment assignment) {
        return assignment.getCourse().getCourseCode() + " - " + assignment.getCourse().getCourseName();
    }

    private String formatSchedule(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "Schedule not set";
        }
        return DATE_FORMAT.format(startDate) + " - " + DATE_FORMAT.format(endDate);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Recently" : DATE_TIME_FORMAT.format(value);
    }

    private LocalDateTime reportTimestamp(PlagiarismReport report) {
        if (report == null) {
            return null;
        }
        return latestOf(report.getCompletedAt(), report.getStartedAt(), report.getUpdatedAt(), report.getCreatedAt());
    }

    private LocalDateTime batchTimestamp(Batch batch) {
        if (batch == null) {
            return null;
        }
        return latestOf(batch.getCompletedAt(), batch.getStartedAt(), batch.getReadyAt(), batch.getUpdatedAt(), batch.getCreatedAt());
    }

    private LocalDateTime latestOf(LocalDateTime... values) {
        return java.util.Arrays.stream(values)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private boolean isActiveBatch(Batch batch) {
        return batch.getStatus() == BatchStatus.QUEUED
                || batch.getStatus() == BatchStatus.READY_FOR_GRADING
                || batch.getStatus() == BatchStatus.RUNNING;
    }

    private boolean isTerminalBatch(Batch batch) {
        return batch.getStatus() == BatchStatus.COMPLETED
                || batch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS;
    }

    private String humanizeBatchStatus(BatchStatus status) {
        return switch (status) {
            case QUEUED -> "Queued";
            case READY_FOR_GRADING -> "Ready";
            case RUNNING -> "Running";
            case COMPLETED_WITH_ERRORS -> "Completed with errors";
            case COMPLETED -> "Completed";
            case PRECHECKED -> "Prechecked";
        };
    }

    private String attentionIcon(String stageKey) {
        return switch (stageKey) {
            case "setup" -> "build_circle";
            case "intake" -> "upload_file";
            case "plagiarism" -> "policy_alert";
            case "batch" -> "manufacturing";
            case "results" -> "lab_profile";
            default -> "info";
        };
    }

    private int compareTimes(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private record AssignmentInsight(
            Assignment assignment,
            String groupLabel,
            int questionCount,
            long testcaseCount,
            int submissionCount,
            int validatedCount,
            int blockedCount,
            Batch latestBatch,
            BatchProgressSummary progress,
            StatusDescriptor status,
            ActivitySeed activity,
            LocalDateTime touchedAt) {
    }

    private record StatusDescriptor(
            String stageKey,
            String stageLabel,
            String statusLabel,
            String statusHelper,
            String actionLabel,
            String actionUrl,
            int priority,
            boolean attention,
            boolean showInTaskTable,
            int blockedCount) {
    }

    private record ActivitySeed(
            LocalDateTime timestamp,
            String icon,
            String title,
            String detail,
            String actionLabel,
            String actionUrl) {
    }

    private record SubmissionSummary(
            int submissionCount,
            int validatedCount,
            int rejectedCount,
            LocalDateTime latestSubmissionAt) {
    }

    private record BatchProgressSummary(
            long totalCount,
            long completedCount) {
    }

    private record StageCounts(
            int setupCount,
            int intakeCount,
            int plagiarismCount,
            int batchCount,
            int resultsCount,
            int totalCount) {
    }
}
