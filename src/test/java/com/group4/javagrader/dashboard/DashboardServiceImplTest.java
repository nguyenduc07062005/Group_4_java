package com.group4.javagrader.dashboard;

import com.group4.javagrader.dto.AssignmentGroupView;
import com.group4.javagrader.dto.CourseBoardView;
import com.group4.javagrader.dto.DashboardView;
import com.group4.javagrader.dto.SemesterBoardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.GradingResultRepository;
import com.group4.javagrader.repository.PlagiarismReportRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.service.TeacherBoardService;
import com.group4.javagrader.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private TeacherBoardService teacherBoardService;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private PlagiarismReportRepository plagiarismReportRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private GradingResultRepository gradingResultRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                teacherBoardService,
                problemRepository,
                testCaseRepository,
                submissionRepository,
                plagiarismReportRepository,
                batchRepository,
                gradingResultRepository);
    }

    @Test
    void buildUsesBulkDashboardLookupsInsteadOfPerAssignmentExpansion() {
        Semester semester = semester(1L, "SP26");
        Course course = course(11L, semester, "SE101");
        Assignment firstAssignment = assignment(101L, semester, course, "Lab 1");
        Assignment secondAssignment = assignment(102L, semester, course, "Lab 2");

        DashboardView dashboard = buildDashboard(
                List.of(firstAssignment, secondAssignment),
                List.of(
                        problem(1001L, firstAssignment, 1),
                        problem(1002L, secondAssignment, 1)),
                List.of(
                        new ProblemTestCaseCountRow(1001L, 2),
                        new ProblemTestCaseCountRow(1002L, 1)),
                List.of(
                        new AssignmentSubmissionStatsRow(101L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 30)),
                        new AssignmentSubmissionStatsRow(102L, 0, 0, 0, null)),
                List.of(plagiarismReport(firstAssignment, LocalDateTime.of(2026, 4, 13, 9, 0), 1)),
                List.of(batch(firstAssignment, 201L, BatchStatus.RUNNING, LocalDateTime.of(2026, 4, 13, 11, 0))),
                List.of(new BatchProgressStatsRow(201L, 4, 1)));

        verify(problemRepository).findByAssignmentIdInAndInternalDefaultFalseOrderByAssignmentIdAscProblemOrderAsc(List.of(101L, 102L));
        verify(problemRepository, never()).findByAssignmentIdAndInternalDefaultFalseOrderByProblemOrderAsc(anyLong());
        verify(testCaseRepository).summarizeByProblemIds(List.of(1001L, 1002L));
        verify(testCaseRepository, never()).countByProblemId(anyLong());
        verify(submissionRepository).summarizeByAssignmentIds(List.of(101L, 102L));
        verify(submissionRepository, never()).findByAssignmentIdOrderByCreatedAtDescIdDesc(anyLong());
        verify(plagiarismReportRepository).findByAssignmentIdInOrderByAssignmentIdAscStartedAtDescIdDesc(List.of(101L, 102L));
        verify(plagiarismReportRepository, never()).findFirstByAssignmentIdOrderByStartedAtDescIdDesc(anyLong());
        verify(batchRepository).findByAssignmentIdInOrderByAssignmentIdAscIdDesc(List.of(101L, 102L));
        verify(batchRepository, never()).findFirstByAssignmentIdOrderByIdDesc(anyLong());

        assertThat(dashboard.getRunningBatches())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getAssignmentName()).isEqualTo("Lab 1");
                    assertThat(item.getProgressLabel()).isEqualTo("1 / 4 submissions completed");
                    assertThat(item.getProgressPercent()).isEqualTo(25);
                });
    }

    @Test
    void buildClassifiesSetupGapsAndResultsReadinessWithoutChangingDashboardOutput() {
        Semester semester = semester(1L, "SP26");
        Course course = course(11L, semester, "SE101");
        Assignment noQuestionAssignment = assignment(101L, semester, course, "No Question Yet");
        Assignment missingCoverageAssignment = assignment(102L, semester, course, "Missing Coverage");
        Assignment readyAssignment = assignment(103L, semester, course, "Ready Results");

        DashboardView dashboard = buildDashboard(
                List.of(noQuestionAssignment, missingCoverageAssignment, readyAssignment),
                List.of(
                        problem(2001L, missingCoverageAssignment, 1),
                        problem(3001L, readyAssignment, 1)),
                List.of(new ProblemTestCaseCountRow(3001L, 2)),
                List.of(
                        new AssignmentSubmissionStatsRow(102L, 0, 0, 0, null),
                        new AssignmentSubmissionStatsRow(103L, 4, 3, 1, LocalDateTime.of(2026, 4, 13, 10, 0))),
                List.of(plagiarismReport(readyAssignment, LocalDateTime.of(2026, 4, 13, 9, 30), 0)),
                List.of(batch(readyAssignment, 301L, BatchStatus.COMPLETED, LocalDateTime.of(2026, 4, 13, 11, 0))),
                List.of());

        assertThat(dashboard.getAttentionItems())
                .extracting(DashboardView.AttentionItem::getTitle)
                .contains("No Question Yet", "Missing Coverage");

        assertThat(dashboard.getTaskRows())
                .extracting(DashboardView.TaskRow::getIssue)
                .contains("Create the first question", "Finish testcase coverage");

        assertThat(dashboard.getRecentActivities())
                .extracting(DashboardView.ActivityItem::getTitle)
                .contains("Batch Completed");

        assertThat(findAssignmentCard(dashboard, 101L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("setup");
                    assertThat(card.getStatusLabel()).isEqualTo("Create the first question");
                    assertThat(card.getPrimaryActionUrl()).isEqualTo("/assignments/101");
                });

        assertThat(findAssignmentCard(dashboard, 102L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("setup");
                    assertThat(card.getStatusLabel()).isEqualTo("Finish testcase coverage");
                });

        assertThat(findAssignmentCard(dashboard, 103L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("results");
                    assertThat(card.getStatusLabel()).isEqualTo("Results ready");
                    assertThat(card.getPrimaryActionUrl()).isEqualTo("/assignments/103/grading#results-overview");
                });
    }

    @Test
    void buildClassifiesPlagiarismAndBatchStatesWithoutChangingDashboardOutput() {
        Semester semester = semester(1L, "SP26");
        Course course = course(11L, semester, "SE101");
        Assignment missingReportAssignment = assignment(201L, semester, course, "Need Report");
        Assignment staleReportAssignment = assignment(202L, semester, course, "Stale Report");
        Assignment blockedAssignment = assignment(203L, semester, course, "Blocked Review");
        Assignment readyBatchAssignment = assignment(204L, semester, course, "Ready Batch");
        Assignment runningBatchAssignment = assignment(205L, semester, course, "Running Batch");

        DashboardView dashboard = buildDashboard(
                List.of(missingReportAssignment, staleReportAssignment, blockedAssignment, readyBatchAssignment, runningBatchAssignment),
                List.of(
                        problem(4001L, missingReportAssignment, 1),
                        problem(4002L, staleReportAssignment, 1),
                        problem(4003L, blockedAssignment, 1),
                        problem(4004L, readyBatchAssignment, 1),
                        problem(4005L, runningBatchAssignment, 1)),
                List.of(
                        new ProblemTestCaseCountRow(4001L, 1),
                        new ProblemTestCaseCountRow(4002L, 1),
                        new ProblemTestCaseCountRow(4003L, 1),
                        new ProblemTestCaseCountRow(4004L, 1),
                        new ProblemTestCaseCountRow(4005L, 1)),
                List.of(
                        new AssignmentSubmissionStatsRow(201L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0)),
                        new AssignmentSubmissionStatsRow(202L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 12, 0)),
                        new AssignmentSubmissionStatsRow(203L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 8, 0)),
                        new AssignmentSubmissionStatsRow(204L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0)),
                        new AssignmentSubmissionStatsRow(205L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0))),
                List.of(
                        plagiarismReport(staleReportAssignment, LocalDateTime.of(2026, 4, 13, 9, 0), 0),
                        plagiarismReport(blockedAssignment, LocalDateTime.of(2026, 4, 13, 9, 0), 2),
                        plagiarismReport(readyBatchAssignment, LocalDateTime.of(2026, 4, 13, 9, 0), 0),
                        plagiarismReport(runningBatchAssignment, LocalDateTime.of(2026, 4, 13, 9, 0), 0)),
                List.of(
                        batch(readyBatchAssignment, 501L, BatchStatus.PRECHECKED, LocalDateTime.of(2026, 4, 13, 11, 0)),
                        batch(runningBatchAssignment, 502L, BatchStatus.RUNNING, LocalDateTime.of(2026, 4, 13, 11, 0))),
                List.of(new BatchProgressStatsRow(502L, 5, 2)));

        assertThat(findAssignmentCard(dashboard, 201L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("plagiarism");
                    assertThat(card.getStatusLabel()).isEqualTo("Run plagiarism before batch");
                });

        assertThat(findAssignmentCard(dashboard, 202L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("plagiarism");
                    assertThat(card.getStatusLabel()).isEqualTo("Plagiarism report is outdated");
                });

        assertThat(findAssignmentCard(dashboard, 203L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("plagiarism");
                    assertThat(card.getStatusLabel()).isEqualTo("2 blocked submission(s)");
                });

        assertThat(findAssignmentCard(dashboard, 204L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("batch");
                    assertThat(card.getStatusLabel()).isEqualTo("Snapshot is ready to start");
                    assertThat(card.getPrimaryActionUrl()).isEqualTo("/assignments/204/grading#snapshot-run");
                });

        assertThat(findAssignmentCard(dashboard, 205L))
                .satisfies(card -> {
                    assertThat(card.getStageKey()).isEqualTo("batch");
                    assertThat(card.getStatusLabel()).isEqualTo("Batch is running");
                });

        assertThat(dashboard.getRunningBatches())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getAssignmentName()).isEqualTo("Running Batch");
                    assertThat(item.getProgressLabel()).isEqualTo("2 / 5 submissions completed");
                    assertThat(item.getProgressPercent()).isEqualTo(40);
                });
    }

    private DashboardView buildDashboard(
            List<Assignment> assignments,
            List<Problem> problems,
            List<TestCaseRepository.ProblemTestCaseCount> testcaseCounts,
            List<SubmissionRepository.AssignmentSubmissionStats> submissionStats,
            List<PlagiarismReport> reports,
            List<Batch> batches,
            List<GradingResultRepository.BatchProgressStats> batchProgressStats) {
        Semester semester = assignments.get(0).getSemester();
        Course course = assignments.get(0).getCourse();
        SemesterBoardView semesterBoard = new SemesterBoardView(
                semester,
                List.of(new CourseBoardView(
                        course,
                        List.of(new AssignmentGroupView(
                                "Week 1",
                                "Grouped assignments",
                                "calendar_view_week",
                                1,
                                assignments)),
                        assignments.size())),
                assignments.size());
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        List<Long> problemIds = problems.stream().map(Problem::getId).toList();
        List<Long> activeBatchIds = batches.stream()
                .filter(batch -> batch.getStatus() == BatchStatus.QUEUED
                        || batch.getStatus() == BatchStatus.READY_FOR_GRADING
                        || batch.getStatus() == BatchStatus.RUNNING)
                .map(Batch::getId)
                .toList();

        when(teacherBoardService.buildActiveSemesterBoard()).thenReturn(List.of(semesterBoard));
        when(problemRepository.findByAssignmentIdInAndInternalDefaultFalseOrderByAssignmentIdAscProblemOrderAsc(assignmentIds)).thenReturn(problems);
        when(testCaseRepository.summarizeByProblemIds(problemIds)).thenReturn(testcaseCounts);
        when(submissionRepository.summarizeByAssignmentIds(assignmentIds)).thenReturn(submissionStats);
        when(plagiarismReportRepository.findByAssignmentIdInOrderByAssignmentIdAscStartedAtDescIdDesc(assignmentIds)).thenReturn(reports);
        when(batchRepository.findByAssignmentIdInOrderByAssignmentIdAscIdDesc(assignmentIds)).thenReturn(batches);
        if (!activeBatchIds.isEmpty()) {
            when(gradingResultRepository.summarizeByBatchIds(activeBatchIds)).thenReturn(batchProgressStats);
        }
        return dashboardService.build();
    }

    @Test
    void buildCourseStatusBarsKeepsStageCountsSortOrderAndLimit() {
        Course alphaCourse = course(11L, semester(1L, "SP26"), "A100");
        Course betaCourse = course(12L, alphaCourse.getSemester(), "B100");
        Course gammaCourse = course(13L, alphaCourse.getSemester(), "C100");
        Course deltaCourse = course(14L, alphaCourse.getSemester(), "D100");
        Course epsilonCourse = course(15L, alphaCourse.getSemester(), "E100");
        Course zetaCourse = course(16L, alphaCourse.getSemester(), "F100");
        Course etaCourse = course(17L, alphaCourse.getSemester(), "G100");
        Course thetaCourse = course(18L, alphaCourse.getSemester(), "H100");
        Course iotaCourse = course(19L, alphaCourse.getSemester(), "I100");
        Course emptyCourse = course(20L, alphaCourse.getSemester(), "Z100");

        Assignment a1 = assignment(1001L, alphaCourse.getSemester(), alphaCourse, "A setup");
        Assignment b1 = assignment(1002L, betaCourse.getSemester(), betaCourse, "B intake");
        Assignment c1 = assignment(1003L, gammaCourse.getSemester(), gammaCourse, "C plagiarism");
        Assignment d1 = assignment(1004L, deltaCourse.getSemester(), deltaCourse, "D batch ready");
        Assignment e1 = assignment(1005L, epsilonCourse.getSemester(), epsilonCourse, "E running");
        Assignment f1 = assignment(1006L, zetaCourse.getSemester(), zetaCourse, "F results");
        Assignment g1 = assignment(1007L, etaCourse.getSemester(), etaCourse, "G intake");
        Assignment h1 = assignment(1008L, thetaCourse.getSemester(), thetaCourse, "H setup");
        Assignment i1 = assignment(1009L, iotaCourse.getSemester(), iotaCourse, "I setup trimmed");

        DashboardView dashboard = buildDashboardForCourses(List.of(
                courseFixture(alphaCourse, List.of(a1), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                courseFixture(betaCourse, List.of(b1), List.of(problem(2002L, b1, 1)), List.of(new ProblemTestCaseCountRow(2002L, 1)),
                        List.of(new AssignmentSubmissionStatsRow(1002L, 1, 1, 0, LocalDateTime.of(2026, 4, 13, 10, 0))), List.of(), List.of(), List.of()),
                courseFixture(gammaCourse, List.of(c1), List.of(problem(2003L, c1, 1)), List.of(new ProblemTestCaseCountRow(2003L, 1)),
                        List.of(new AssignmentSubmissionStatsRow(1003L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0))),
                        List.of(), List.of(), List.of()),
                courseFixture(deltaCourse, List.of(d1), List.of(problem(2004L, d1, 1)), List.of(new ProblemTestCaseCountRow(2004L, 1)),
                        List.of(new AssignmentSubmissionStatsRow(1004L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0))),
                        List.of(plagiarismReport(d1, LocalDateTime.of(2026, 4, 13, 9, 0), 0)),
                        List.of(batch(d1, 3004L, BatchStatus.PRECHECKED, LocalDateTime.of(2026, 4, 13, 11, 0))),
                        List.of()),
                courseFixture(epsilonCourse, List.of(e1), List.of(problem(2005L, e1, 1)), List.of(new ProblemTestCaseCountRow(2005L, 1)),
                        List.of(new AssignmentSubmissionStatsRow(1005L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0))),
                        List.of(plagiarismReport(e1, LocalDateTime.of(2026, 4, 13, 9, 0), 0)),
                        List.of(batch(e1, 3005L, BatchStatus.RUNNING, LocalDateTime.of(2026, 4, 13, 11, 0))),
                        List.of(new BatchProgressStatsRow(3005L, 5, 2))),
                courseFixture(zetaCourse, List.of(f1), List.of(problem(2006L, f1, 1)), List.of(new ProblemTestCaseCountRow(2006L, 1)),
                        List.of(new AssignmentSubmissionStatsRow(1006L, 3, 2, 1, LocalDateTime.of(2026, 4, 13, 10, 0))),
                        List.of(plagiarismReport(f1, LocalDateTime.of(2026, 4, 13, 9, 0), 0)),
                        List.of(batch(f1, 3006L, BatchStatus.COMPLETED, LocalDateTime.of(2026, 4, 13, 11, 0))),
                        List.of()),
                courseFixture(etaCourse, List.of(g1), List.of(problem(2007L, g1, 1)), List.of(new ProblemTestCaseCountRow(2007L, 1)),
                        List.of(new AssignmentSubmissionStatsRow(1007L, 1, 1, 0, LocalDateTime.of(2026, 4, 13, 10, 0))), List.of(), List.of(), List.of()),
                courseFixture(thetaCourse, List.of(h1), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                courseFixture(iotaCourse, List.of(i1), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                courseFixture(emptyCourse, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));

        assertThat(dashboard.getCourseStatusBars()).hasSize(8);
        assertThat(dashboard.getCourseStatusBars())
                .extracting(DashboardView.CourseStatusBar::getCourseLabel)
                .containsExactly(
                        "A100 - A100 Name",
                        "B100 - B100 Name",
                        "C100 - C100 Name",
                        "D100 - D100 Name",
                        "E100 - E100 Name",
                        "F100 - F100 Name",
                        "G100 - G100 Name",
                        "H100 - H100 Name");

        assertThat(dashboard.getCourseStatusBars().get(0))
                .satisfies(bar -> {
                    assertThat(bar.getSetupCount()).isEqualTo(1);
                    assertThat(bar.getTotalCount()).isEqualTo(1);
                });
        assertThat(dashboard.getCourseStatusBars().get(1).getIntakeCount()).isEqualTo(1);
        assertThat(dashboard.getCourseStatusBars().get(2).getPlagiarismCount()).isEqualTo(1);
        assertThat(dashboard.getCourseStatusBars().get(3).getBatchCount()).isEqualTo(1);
        assertThat(dashboard.getCourseStatusBars().get(4).getBatchCount()).isEqualTo(1);
        assertThat(dashboard.getCourseStatusBars().get(5).getResultsCount()).isEqualTo(1);
        assertThat(dashboard.getCourseStatusBars().stream().map(DashboardView.CourseStatusBar::getCourseLabel))
                .doesNotContain("I100 - I100 Name", "Z100 - Z100 Name");
    }

    private DashboardView.AssignmentCard findAssignmentCard(DashboardView dashboard, Long assignmentId) {
        return dashboard.getSemesters().stream()
                .flatMap(semester -> semester.getCourses().stream())
                .flatMap(course -> course.getLanes().stream())
                .flatMap(lane -> lane.getAssignments().stream())
                .filter(card -> card.getId().equals(assignmentId))
                .findFirst()
                .orElseThrow();
    }

    private DashboardView buildDashboardForCourses(List<CourseFixture> fixtures) {
        List<Assignment> assignments = fixtures.stream().flatMap(fixture -> fixture.assignments().stream()).toList();
        List<Problem> problems = fixtures.stream().flatMap(fixture -> fixture.problems().stream()).toList();
        List<TestCaseRepository.ProblemTestCaseCount> testcaseCounts = fixtures.stream().flatMap(fixture -> fixture.testcaseCounts().stream()).toList();
        List<SubmissionRepository.AssignmentSubmissionStats> submissionStats = fixtures.stream().flatMap(fixture -> fixture.submissionStats().stream()).toList();
        List<PlagiarismReport> reports = fixtures.stream().flatMap(fixture -> fixture.reports().stream()).toList();
        List<Batch> batches = fixtures.stream().flatMap(fixture -> fixture.batches().stream()).toList();
        List<GradingResultRepository.BatchProgressStats> batchProgressStats = fixtures.stream().flatMap(fixture -> fixture.batchProgressStats().stream()).toList();

        Semester semester = fixtures.get(0).course().getSemester();
        List<CourseBoardView> courseBoards = fixtures.stream()
                .map(fixture -> new CourseBoardView(
                        fixture.course(),
                        List.of(new AssignmentGroupView(
                                "Week 1",
                                "Grouped assignments",
                                "calendar_view_week",
                                1,
                                fixture.assignments())),
                        fixture.assignments().size()))
                .toList();
        SemesterBoardView semesterBoard = new SemesterBoardView(semester, courseBoards, assignments.size());
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        List<Long> problemIds = problems.stream().map(Problem::getId).toList();
        List<Long> activeBatchIds = batches.stream()
                .filter(batch -> batch.getStatus() == BatchStatus.QUEUED
                        || batch.getStatus() == BatchStatus.READY_FOR_GRADING
                        || batch.getStatus() == BatchStatus.RUNNING)
                .map(Batch::getId)
                .toList();

        when(teacherBoardService.buildActiveSemesterBoard()).thenReturn(List.of(semesterBoard));
        when(problemRepository.findByAssignmentIdInAndInternalDefaultFalseOrderByAssignmentIdAscProblemOrderAsc(assignmentIds)).thenReturn(problems);
        when(testCaseRepository.summarizeByProblemIds(problemIds)).thenReturn(testcaseCounts);
        when(submissionRepository.summarizeByAssignmentIds(assignmentIds)).thenReturn(submissionStats);
        when(plagiarismReportRepository.findByAssignmentIdInOrderByAssignmentIdAscStartedAtDescIdDesc(assignmentIds)).thenReturn(reports);
        when(batchRepository.findByAssignmentIdInOrderByAssignmentIdAscIdDesc(assignmentIds)).thenReturn(batches);
        if (!activeBatchIds.isEmpty()) {
            when(gradingResultRepository.summarizeByBatchIds(activeBatchIds)).thenReturn(batchProgressStats);
        }
        return dashboardService.build();
    }
    private CourseFixture courseFixture(
            Course course,
            List<Assignment> assignments,
            List<Problem> problems,
            List<TestCaseRepository.ProblemTestCaseCount> testcaseCounts,
            List<SubmissionRepository.AssignmentSubmissionStats> submissionStats,
            List<PlagiarismReport> reports,
            List<Batch> batches,
            List<GradingResultRepository.BatchProgressStats> batchProgressStats) {
        return new CourseFixture(course, assignments, problems, testcaseCounts, submissionStats, reports, batches, batchProgressStats);
    }

    private Semester semester(Long id, String code) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setCode(code);
        semester.setName("Spring 2026");
        semester.setStartDate(LocalDate.of(2026, 1, 6));
        semester.setEndDate(LocalDate.of(2026, 5, 30));
        return semester;
    }

    private Course course(Long id, Semester semester, String courseCode) {
        Course course = new Course();
        course.setId(id);
        course.setSemester(semester);
        course.setCourseCode(courseCode);
        course.setCourseName(courseCode + " Name");
        course.setWeekCount(12);
        return course;
    }

    private Assignment assignment(Long id, Semester semester, Course course, String name) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setSemester(semester);
        assignment.setCourse(course);
        assignment.setAssignmentName(name);
        assignment.setAssignmentType(com.group4.javagrader.entity.AssignmentType.CUSTOM);
        assignment.setGradingMode(GradingMode.JAVA_CORE);
        assignment.setPlagiarismThreshold(BigDecimal.valueOf(80));
        assignment.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        assignment.setDisplayOrder(1000);
        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);
        return assignment;
    }

    private Problem problem(Long id, Assignment assignment, int order) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setAssignment(assignment);
        problem.setProblemOrder(order);
        problem.setTitle("Problem " + order);
        problem.setMaxScore(BigDecimal.TEN);
        problem.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problem.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        problem.setInternalDefault(false);
        return problem;
    }

    private PlagiarismReport plagiarismReport(Assignment assignment, LocalDateTime completedAt, int blockedCount) {
        PlagiarismReport report = new PlagiarismReport();
        report.setAssignment(assignment);
        report.setStatus("COMPLETED");
        report.setThreshold(BigDecimal.valueOf(80));
        report.setTotalSubmissions(3);
        report.setFlaggedPairCount(1);
        report.setBlockedSubmissionCount(blockedCount);
        report.setStartedAt(completedAt.minusMinutes(5));
        report.setCompletedAt(completedAt);
        return report;
    }

    private Batch batch(Assignment assignment, Long id, BatchStatus status, LocalDateTime startedAt) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setAssignment(assignment);
        batch.setStatus(status);
        batch.setQueueCapacity(8);
        batch.setWorkerCount(2);
        batch.setTotalSubmissions(4);
        batch.setGradeableSubmissionCount(4);
        batch.setExcludedSubmissionCount(0);
        batch.setStartedAt(startedAt);
        return batch;
    }

    private static final class ProblemTestCaseCountRow implements TestCaseRepository.ProblemTestCaseCount {
        private final Long problemId;
        private final long testcaseCount;

        private ProblemTestCaseCountRow(Long problemId, long testcaseCount) {
            this.problemId = problemId;
            this.testcaseCount = testcaseCount;
        }

        @Override
        public Long getProblemId() {
            return problemId;
        }

        @Override
        public long getTestcaseCount() {
            return testcaseCount;
        }
    }

    private static final class AssignmentSubmissionStatsRow implements SubmissionRepository.AssignmentSubmissionStats {
        private final Long assignmentId;
        private final long submissionCount;
        private final long validatedCount;
        private final long rejectedCount;
        private final LocalDateTime latestSubmissionAt;

        private AssignmentSubmissionStatsRow(
                Long assignmentId,
                long submissionCount,
                long validatedCount,
                long rejectedCount,
                LocalDateTime latestSubmissionAt) {
            this.assignmentId = assignmentId;
            this.submissionCount = submissionCount;
            this.validatedCount = validatedCount;
            this.rejectedCount = rejectedCount;
            this.latestSubmissionAt = latestSubmissionAt;
        }

        @Override
        public Long getAssignmentId() {
            return assignmentId;
        }

        @Override
        public long getSubmissionCount() {
            return submissionCount;
        }

        @Override
        public long getValidatedCount() {
            return validatedCount;
        }

        @Override
        public long getRejectedCount() {
            return rejectedCount;
        }

        @Override
        public LocalDateTime getLatestSubmissionAt() {
            return latestSubmissionAt;
        }
    }

    private static final class BatchProgressStatsRow implements GradingResultRepository.BatchProgressStats {
        private final Long batchId;
        private final long totalCount;
        private final long completedCount;

        private BatchProgressStatsRow(Long batchId, long totalCount, long completedCount) {
            this.batchId = batchId;
            this.totalCount = totalCount;
            this.completedCount = completedCount;
        }

        @Override
        public Long getBatchId() {
            return batchId;
        }

        @Override
        public long getTotalCount() {
            return totalCount;
        }

        @Override
        public long getCompletedCount() {
            return completedCount;
        }
    }

    private record CourseFixture(
            Course course,
            List<Assignment> assignments,
            List<Problem> problems,
            List<TestCaseRepository.ProblemTestCaseCount> testcaseCounts,
            List<SubmissionRepository.AssignmentSubmissionStats> submissionStats,
            List<PlagiarismReport> reports,
            List<Batch> batches,
            List<GradingResultRepository.BatchProgressStats> batchProgressStats) {
    }
}
