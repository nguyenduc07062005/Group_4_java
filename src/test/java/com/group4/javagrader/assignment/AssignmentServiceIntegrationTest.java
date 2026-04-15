package com.group4.javagrader.assignment;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.AssignmentWorkspaceForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.WorkspaceProblemForm;
import com.group4.javagrader.dto.WorkspaceTestCaseForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputComparisonMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.AssignmentAttachmentRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.TestCaseService;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AssignmentServiceIntegrationTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentAttachmentRepository assignmentAttachmentRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Test
    void repositoryFindByIdDoesNotInitializeAssignmentParentsByDefault() {
        Long semesterId = createSemester("LAZY26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Lean Assignment Read");
        form.setSemesterId(semesterId);
        form.setGradingMode(GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(OutputNormalizationPolicy.STRICT);

        Long assignmentId = assignmentService.create(form);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();

        assertThat(Hibernate.isInitialized(assignment.getSemester())).isFalse();
        assertThat(Hibernate.isInitialized(assignment.getCourse())).isFalse();
    }

    @Test
    void assignmentServiceReadModelsInitializeAssignmentParentsForViewUse() {
        Long semesterId = createSemester("VIEW26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("View Assignment Read");
        form.setSemesterId(semesterId);
        form.setGradingMode(GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(OutputNormalizationPolicy.STRICT);

        Long assignmentId = assignmentService.create(form);

        Assignment detailAssignment = assignmentService.findById(assignmentId).orElseThrow();
        assertThat(Hibernate.isInitialized(detailAssignment.getSemester())).isTrue();
        assertThat(Hibernate.isInitialized(detailAssignment.getCourse())).isTrue();

        Assignment listAssignment = assignmentService.findBySemesterIds(List.of(semesterId)).get(0);
        assertThat(Hibernate.isInitialized(listAssignment.getSemester())).isTrue();
        assertThat(Hibernate.isInitialized(listAssignment.getCourse())).isTrue();
    }

    @Test
    void createStoresPhaseTwoAssignmentConfiguration() {
        Long semesterId = createSemester("SP26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Collections Lab");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        form.setPlagiarismThreshold(35);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.TRIM_ALL);
        form.setInputMode(com.group4.javagrader.entity.InputMode.FILE);
        form.setLogicWeight(60);
        form.setOopWeight(40);
        form.setDescriptionFile(new MockMultipartFile(
                "descriptionFile",
                "brief.md",
                "text/markdown",
                "# Collections Lab".getBytes(StandardCharsets.UTF_8)));
        form.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                "{\"rules\":[]}".getBytes(StandardCharsets.UTF_8)));

        Long assignmentId = assignmentService.create(form);

        Assignment assignment = assignmentRepository.findByIdWithSemesterAndCourse(assignmentId).orElseThrow();
        assertThat(assignment.getAssignmentName()).isEqualTo("Collections Lab");
        assertThat(assignment.getGradingMode()).isEqualTo(GradingMode.OOP);
        assertThat(assignment.getPlagiarismThreshold()).isEqualByComparingTo("35.00");
        assertThat(assignment.getOutputNormalizationPolicy()).isEqualTo(OutputNormalizationPolicy.TRIM_ALL);
        assertThat(assignment.getLogicWeight()).isEqualTo(60);
        assertThat(assignment.getOopWeight()).isEqualTo(40);
        assertThat(assignment.getDescriptionFileName()).isEqualTo("brief.md");
        assertThat(assignment.getOopRuleConfigFileName()).isEqualTo("rules.json");
        assertThat(assignment.getSemester().getCode()).isEqualTo("SP26");
        assertThat(assignment.getCourse()).isNotNull();
        assertThat(assignment.getCourse().getCourseCode()).isEqualTo("GENERAL");
        assertThat(assignment.getDescription()).contains("Collections Lab");

        Problem defaultRuntime = problemRepository.findByAssignmentIdOrderByProblemOrderAsc(assignmentId).get(0);
        assertThat(defaultRuntime.isInternalDefault()).isTrue();
        assertThat(defaultRuntime.getTitle()).isEqualTo("Assignment Runtime");
        assertThat(defaultRuntime.getInputMode()).isEqualTo(InputMode.FILE);
        assertThat(defaultRuntime.getOutputComparisonMode()).isEqualTo(OutputComparisonMode.TRIM_ALL);
        assertThat(defaultRuntime.getMaxScore()).isEqualByComparingTo("100.00");
    }

    @Test
    void createRejectsUnsupportedDescriptionUpload() {
        Long semesterId = createSemester("SU26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Broken Upload");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        form.setLogicWeight(50);
        form.setOopWeight(50);
        form.setDescriptionFile(new MockMultipartFile(
                "descriptionFile",
                "brief.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}));

        assertThatThrownBy(() -> assignmentService.create(form))
                .isInstanceOf(AssignmentConfigValidationException.class)
                .hasMessage("Assignment description must be a .pdf or .md file.");
    }

    @Test
    void createJavaCoreAssignmentIgnoresOopOnlyConfiguration() {
        Long semesterId = createSemester("FA26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Core Lab");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(25);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        form.setLogicWeight(35);
        form.setOopWeight(65);
        form.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                "{\"rules\":[\"unused\"]}".getBytes(StandardCharsets.UTF_8)));

        Long assignmentId = assignmentService.create(form);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(assignment.getGradingMode()).isEqualTo(GradingMode.JAVA_CORE);
        assertThat(assignment.getLogicWeight()).isEqualTo(100);
        assertThat(assignment.getOopWeight()).isEqualTo(0);
        assertThat(assignment.getOopRuleConfigFileName()).isNull();
        assertThat(assignment.getOopRuleConfigContentType()).isNull();
        assertThat(assignmentAttachmentRepository
                .findByAssignmentIdAndAttachmentType(assignmentId, AssignmentAttachmentType.OOP_RULE_CONFIG))
                .isEmpty();
    }

    @Test
    void createAttachesAssignmentToSelectedCourseAndStoresWeeklyMetadata() {
        Long semesterId = createSemester("CRS26");
        Semester semester = semesterRepository.findById(semesterId).orElseThrow();

        Course course = new Course();
        course.setSemester(semester);
        course.setCourseCode("SWE201");
        course.setCourseName("Software Engineering");
        course.setWeekCount(10);
        course.setArchived(false);
        course = courseRepository.save(course);

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Tuan 3");
        form.setSemesterId(semesterId);
        form.setCourseId(course.getId());
        form.setAssignmentType(com.group4.javagrader.entity.AssignmentType.WEEKLY);
        form.setWeekNumber(3);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);

        Long assignmentId = assignmentService.create(form);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(assignment.getCourse().getId()).isEqualTo(course.getId());
        assertThat(assignment.getSemester().getId()).isEqualTo(semesterId);
        assertThat(assignment.getAssignmentType()).isEqualTo(AssignmentType.WEEKLY);
        assertThat(assignment.getWeekNumber()).isEqualTo(3);
        assertThat(assignment.getDisplayOrder()).isEqualTo(103);
    }

    @Test
    void createAllowsMultipleWeeklyAssignmentsInSameWeek() {
        Long semesterId = createSemester("CRS27");
        Semester semester = semesterRepository.findById(semesterId).orElseThrow();

        Course course = new Course();
        course.setSemester(semester);
        course.setCourseCode("SWE301");
        course.setCourseName("Advanced Software Engineering");
        course.setWeekCount(12);
        course.setArchived(false);
        course = courseRepository.save(course);

        AssignmentForm first = new AssignmentForm();
        first.setAssignmentName("Week 4 - Lab");
        first.setSemesterId(semesterId);
        first.setCourseId(course.getId());
        first.setAssignmentType(com.group4.javagrader.entity.AssignmentType.WEEKLY);
        first.setWeekNumber(4);
        first.setGradingMode(GradingMode.JAVA_CORE);
        first.setPlagiarismThreshold(20);
        first.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);

        AssignmentForm second = new AssignmentForm();
        second.setAssignmentName("Week 4 - Quiz");
        second.setSemesterId(semesterId);
        second.setCourseId(course.getId());
        second.setAssignmentType(com.group4.javagrader.entity.AssignmentType.WEEKLY);
        second.setWeekNumber(4);
        second.setGradingMode(GradingMode.JAVA_CORE);
        second.setPlagiarismThreshold(20);
        second.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);

        assignmentService.create(first);
        assignmentService.create(second);

        List<Assignment> assignments = assignmentRepository.findByCourseIdOrderByDisplayOrderAscIdAsc(course.getId());
        assertThat(assignments).hasSize(2);
        assertThat(assignments).extracting(Assignment::getAssignmentName)
                .containsExactly("Week 4 - Lab", "Week 4 - Quiz");
        assertThat(assignments).extracting(Assignment::getWeekNumber)
                .containsExactly(4, 4);
        assertThat(assignments).extracting(Assignment::getDisplayOrder)
                .containsExactly(104, 104);
    }

    @Test
    void updateChangesConfigurationAndPreservesExistingUploads() {
        Long semesterId = createSemester("UP26");

        AssignmentForm createForm = new AssignmentForm();
        createForm.setAssignmentName("Original Lab");
        createForm.setSemesterId(semesterId);
        createForm.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        createForm.setPlagiarismThreshold(35);
        createForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.TRIM_ALL);
        createForm.setInputMode(com.group4.javagrader.entity.InputMode.FILE);
        createForm.setLogicWeight(60);
        createForm.setOopWeight(40);
        createForm.setDescriptionFile(new MockMultipartFile(
                "descriptionFile",
                "brief.md",
                "text/markdown",
                "# Original Lab".getBytes(StandardCharsets.UTF_8)));
        createForm.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                "{\"rules\":[]}".getBytes(StandardCharsets.UTF_8)));

        Long assignmentId = assignmentService.create(createForm);

        AssignmentForm updateForm = new AssignmentForm();
        updateForm.setAssignmentName("Updated Lab");
        updateForm.setSemesterId(semesterId);
        updateForm.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        updateForm.setPlagiarismThreshold(45);
        updateForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        updateForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        updateForm.setLogicWeight(70);
        updateForm.setOopWeight(30);

        assignmentService.update(assignmentId, updateForm);

        Assignment updated = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(updated.getAssignmentName()).isEqualTo("Updated Lab");
        assertThat(updated.getPlagiarismThreshold()).isEqualByComparingTo("45.00");
        assertThat(updated.getOutputNormalizationPolicy()).isEqualTo(OutputNormalizationPolicy.STRICT);
        assertThat(updated.getLogicWeight()).isEqualTo(70);
        assertThat(updated.getOopWeight()).isEqualTo(30);
        assertThat(updated.getDescriptionFileName()).isEqualTo("brief.md");
        assertThat(updated.getOopRuleConfigFileName()).isEqualTo("rules.json");

        Problem defaultRuntime = problemRepository.findByAssignmentIdOrderByProblemOrderAsc(assignmentId).get(0);
        assertThat(defaultRuntime.isInternalDefault()).isTrue();
        assertThat(defaultRuntime.getInputMode()).isEqualTo(InputMode.STDIN);
        assertThat(defaultRuntime.getOutputComparisonMode()).isEqualTo(OutputComparisonMode.EXACT);
    }

    @Test
    void saveWorkspaceUpdatesExistingProblemAndTestcaseInPlace() {
        Long semesterId = createSemester("WS26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Workspace Lab");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(25);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);

        Long assignmentId = assignmentService.create(form);

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Warmup");
        problemForm.setMaxScore(java.math.BigDecimal.TEN);
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long problemId = problemService.create(problemForm);

        TestCase initialTestCase = new TestCase();
        initialTestCase.setProblem(problemRepository.findById(problemId).orElseThrow());
        initialTestCase.setCaseOrder(1);
        initialTestCase.setInputData("1 2");
        initialTestCase.setExpectedOutput("3");
        initialTestCase.setWeight(java.math.BigDecimal.ONE);
        initialTestCase.setSample(false);
        Long testCaseId = testCaseRepository.save(initialTestCase).getId();

        AssignmentWorkspaceForm workspaceForm = assignmentService.loadWorkspace(assignmentId);
        workspaceForm.setAssignmentName("Workspace Lab Updated");
        workspaceForm.setPlagiarismThreshold(30);

        WorkspaceProblemForm workspaceProblem = workspaceForm.getProblems().get(0);
        workspaceProblem.setTitle("Warmup Updated");
        workspaceProblem.setMaxScore(java.math.BigDecimal.valueOf(15));
        workspaceProblem.setInputMode(com.group4.javagrader.entity.InputMode.FILE);

        WorkspaceTestCaseForm workspaceTestCase = workspaceProblem.getTestCases().get(0);
        workspaceTestCase.setInputData("5 5");
        workspaceTestCase.setExpectedOutput("10");
        workspaceTestCase.setWeight(java.math.BigDecimal.valueOf(2));

        Long savedAssignmentId = assignmentService.saveWorkspace(workspaceForm);

        assertThat(savedAssignmentId).isEqualTo(assignmentId);

        Assignment updatedAssignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(updatedAssignment.getAssignmentName()).isEqualTo("Workspace Lab Updated");
        assertThat(updatedAssignment.getPlagiarismThreshold()).isEqualByComparingTo("30.00");

        Problem updatedProblem = problemRepository.findById(problemId).orElseThrow();
        assertThat(updatedProblem.getId()).isEqualTo(problemId);
        assertThat(updatedProblem.getTitle()).isEqualTo("Warmup Updated");
        assertThat(updatedProblem.getMaxScore()).isEqualByComparingTo("15");
        assertThat(updatedProblem.getInputMode()).isEqualTo(InputMode.FILE);

        TestCase updatedTestCase = testCaseRepository.findById(testCaseId).orElseThrow();
        assertThat(updatedTestCase.getId()).isEqualTo(testCaseId);
        assertThat(updatedTestCase.getInputData()).isEqualTo("5 5");
        assertThat(updatedTestCase.getExpectedOutput()).isEqualTo("10");
        assertThat(updatedTestCase.getWeight()).isEqualByComparingTo("2");
    }

    @Test
    void saveWorkspaceRejectsProblemIdFromAnotherAssignment() {
        Long semesterId = createSemester("WS27");

        AssignmentForm firstAssignmentForm = new AssignmentForm();
        firstAssignmentForm.setAssignmentName("First Workspace");
        firstAssignmentForm.setSemesterId(semesterId);
        firstAssignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        firstAssignmentForm.setPlagiarismThreshold(20);
        firstAssignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long firstAssignmentId = assignmentService.create(firstAssignmentForm);

        AssignmentForm secondAssignmentForm = new AssignmentForm();
        secondAssignmentForm.setAssignmentName("Second Workspace");
        secondAssignmentForm.setSemesterId(semesterId);
        secondAssignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        secondAssignmentForm.setPlagiarismThreshold(20);
        secondAssignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long secondAssignmentId = assignmentService.create(secondAssignmentForm);

        ProblemForm secondProblemForm = new ProblemForm();
        secondProblemForm.setAssignmentId(secondAssignmentId);
        secondProblemForm.setTitle("Foreign Problem");
        secondProblemForm.setMaxScore(java.math.BigDecimal.TEN);
        secondProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        secondProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long foreignProblemId = problemService.create(secondProblemForm);

        AssignmentWorkspaceForm workspaceForm = assignmentService.loadWorkspace(firstAssignmentId);
        WorkspaceProblemForm injectedProblem = new WorkspaceProblemForm();
        injectedProblem.setId(foreignProblemId);
        injectedProblem.setTitle("Injected");
        injectedProblem.setMaxScore(java.math.BigDecimal.ONE);
        injectedProblem.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        injectedProblem.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        workspaceForm.setProblems(List.of(injectedProblem));

        assertThatThrownBy(() -> assignmentService.saveWorkspace(workspaceForm))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Problem does not belong to this assignment.");
    }

    @Test
    void saveWorkspaceRejectsTestcaseIdFromAnotherProblem() {
        Long semesterId = createSemester("WS28");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Workspace Guard");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long assignmentId = assignmentService.create(form);

        ProblemForm firstProblemForm = new ProblemForm();
        firstProblemForm.setAssignmentId(assignmentId);
        firstProblemForm.setTitle("Problem One");
        firstProblemForm.setMaxScore(java.math.BigDecimal.TEN);
        firstProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        firstProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long firstProblemId = problemService.create(firstProblemForm);

        ProblemForm secondProblemForm = new ProblemForm();
        secondProblemForm.setAssignmentId(assignmentId);
        secondProblemForm.setTitle("Problem Two");
        secondProblemForm.setMaxScore(java.math.BigDecimal.TEN);
        secondProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        secondProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long secondProblemId = problemService.create(secondProblemForm);

        TestCase foreignTestCase = new TestCase();
        foreignTestCase.setProblem(problemRepository.findById(secondProblemId).orElseThrow());
        foreignTestCase.setCaseOrder(1);
        foreignTestCase.setInputData("2 2");
        foreignTestCase.setExpectedOutput("4");
        foreignTestCase.setWeight(java.math.BigDecimal.ONE);
        foreignTestCase.setSample(false);
        Long foreignTestCaseId = testCaseRepository.save(foreignTestCase).getId();

        AssignmentWorkspaceForm workspaceForm = assignmentService.loadWorkspace(assignmentId);
        WorkspaceProblemForm firstProblem = workspaceForm.getProblems().stream()
                .filter(problem -> problem.getId().equals(firstProblemId))
                .findFirst()
                .orElseThrow();

        WorkspaceTestCaseForm injectedTestCase = new WorkspaceTestCaseForm();
        injectedTestCase.setId(foreignTestCaseId);
        injectedTestCase.setInputData("9 9");
        injectedTestCase.setExpectedOutput("18");
        injectedTestCase.setWeight(java.math.BigDecimal.valueOf(3));
        firstProblem.setTestCases(List.of(injectedTestCase));

        assertThatThrownBy(() -> assignmentService.saveWorkspace(workspaceForm))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Testcase does not belong to this problem.");
    }

    @Test
    void syncAssignmentSettingsKeepsVisibleQuestionsUntouchedWhenMultipleQuestionsExist() {
        Long semesterId = createSemester("WS29");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Workspace Contract");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long assignmentId = assignmentService.create(form);

        ProblemForm firstProblemForm = new ProblemForm();
        firstProblemForm.setAssignmentId(assignmentId);
        firstProblemForm.setTitle("Question One");
        firstProblemForm.setMaxScore(java.math.BigDecimal.valueOf(15));
        firstProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        firstProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long firstProblemId = problemService.create(firstProblemForm);

        ProblemForm secondProblemForm = new ProblemForm();
        secondProblemForm.setAssignmentId(assignmentId);
        secondProblemForm.setTitle("Question Two");
        secondProblemForm.setMaxScore(java.math.BigDecimal.valueOf(20));
        secondProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.FILE);
        secondProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.TRIM_ALL);
        Long secondProblemId = problemService.create(secondProblemForm);

        problemService.syncPrimaryProblemFromAssignment(
                assignmentId,
                "Assignment Runtime Defaults",
                java.math.BigDecimal.valueOf(88),
                InputMode.FILE,
                OutputNormalizationPolicy.TRIM_ALL);

        Problem firstProblem = problemRepository.findById(firstProblemId).orElseThrow();
        Problem secondProblem = problemRepository.findById(secondProblemId).orElseThrow();
        assertThat(firstProblem.getTitle()).isEqualTo("Question One");
        assertThat(firstProblem.getMaxScore()).isEqualByComparingTo("15");
        assertThat(firstProblem.getInputMode()).isEqualTo(InputMode.STDIN);
        assertThat(firstProblem.getOutputComparisonMode()).isEqualTo(OutputComparisonMode.EXACT);
        assertThat(secondProblem.getTitle()).isEqualTo("Question Two");
        assertThat(secondProblem.getMaxScore()).isEqualByComparingTo("20");
        assertThat(secondProblem.getInputMode()).isEqualTo(InputMode.FILE);
        assertThat(secondProblem.getOutputComparisonMode()).isEqualTo(OutputComparisonMode.TRIM_ALL);

        Problem runtimeDefaults = problemRepository
                .findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(assignmentId)
                .orElseThrow();
        assertThat(runtimeDefaults.getTitle()).isEqualTo("Assignment Runtime Defaults");
        assertThat(runtimeDefaults.getMaxScore()).isEqualByComparingTo("88");
        assertThat(runtimeDefaults.getInputMode()).isEqualTo(InputMode.FILE);
        assertThat(runtimeDefaults.getOutputComparisonMode()).isEqualTo(OutputComparisonMode.TRIM_ALL);
    }

    @Test
    void deleteProblemRejectsQuestionWhenTestcasesStillExist() {
        Long semesterId = createSemester("DELP26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Delete Guard");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long assignmentId = assignmentService.create(form);

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Question With Testcases");
        problemForm.setMaxScore(java.math.BigDecimal.TEN);
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long problemId = problemService.create(problemForm);

        TestCase testCase = new TestCase();
        testCase.setProblem(problemRepository.findById(problemId).orElseThrow());
        testCase.setCaseOrder(1);
        testCase.setInputData("1 2");
        testCase.setExpectedOutput("3");
        testCase.setWeight(java.math.BigDecimal.ONE);
        testCase.setSample(false);
        testCaseRepository.save(testCase);

        assertThatThrownBy(() -> problemService.delete(assignmentId, problemId))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Remove testcases before deleting this question.");
    }

    @Test
    void deleteTestcaseRejectsForeignOwnership() {
        Long semesterId = createSemester("DELT26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Delete Testcase Guard");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long assignmentId = assignmentService.create(form);

        ProblemForm firstProblemForm = new ProblemForm();
        firstProblemForm.setAssignmentId(assignmentId);
        firstProblemForm.setTitle("First Question");
        firstProblemForm.setMaxScore(java.math.BigDecimal.TEN);
        firstProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        firstProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long firstProblemId = problemService.create(firstProblemForm);

        ProblemForm secondProblemForm = new ProblemForm();
        secondProblemForm.setAssignmentId(assignmentId);
        secondProblemForm.setTitle("Second Question");
        secondProblemForm.setMaxScore(java.math.BigDecimal.TEN);
        secondProblemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        secondProblemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long secondProblemId = problemService.create(secondProblemForm);

        TestCase foreignTestCase = new TestCase();
        foreignTestCase.setProblem(problemRepository.findById(secondProblemId).orElseThrow());
        foreignTestCase.setCaseOrder(1);
        foreignTestCase.setInputData("2 2");
        foreignTestCase.setExpectedOutput("4");
        foreignTestCase.setWeight(java.math.BigDecimal.ONE);
        foreignTestCase.setSample(false);
        Long foreignTestCaseId = testCaseRepository.save(foreignTestCase).getId();

        assertThatThrownBy(() -> testCaseService.delete(firstProblemId, foreignTestCaseId))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Testcase does not belong to this problem.");
    }

    @Test
    void deleteRemovesAssignmentWhenNoSetupExists() {
        Long semesterId = createSemester("DL26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Delete Me");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);

        Long assignmentId = assignmentService.create(form);

        assignmentService.delete(assignmentId);

        assertThat(assignmentRepository.findById(assignmentId)).isEmpty();
    }

    @Test
    void deleteRejectsAssignmentAfterSetupStarts() {
        Long semesterId = createSemester("LOCK26");

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Protected Lab");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);

        Long assignmentId = assignmentService.create(form);

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Warmup");
        problemForm.setMaxScore(java.math.BigDecimal.TEN);
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        problemService.create(problemForm);

        assertThatThrownBy(() -> assignmentService.delete(assignmentId))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Cannot delete assignment after setup has started.");
    }

    @Test
    void updateRejectsMissingAssignmentAsResourceNotFound() {
        AssignmentForm form = new AssignmentForm();

        assertThatThrownBy(() -> assignmentService.update(999_999L, form))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void deleteRejectsMissingAssignmentAsResourceNotFound() {
        assertThatThrownBy(() -> assignmentService.delete(999_998L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void createProblemRejectsMissingAssignmentAsResourceNotFound() {
        ProblemForm problemForm = problemForm(999_997L, "Missing Assignment Question");

        assertThatThrownBy(() -> problemService.create(problemForm))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found.");
    }

    @Test
    void updateProblemRejectsMissingProblemAsResourceNotFound() {
        Long semesterId = createSemester("MP26");
        Long assignmentId = createAssignment(semesterId, "Missing Problem Guard");
        ProblemForm problemForm = problemForm(assignmentId, "Missing Problem");

        assertThatThrownBy(() -> problemService.update(999_996L, problemForm))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Problem not found.");
    }

    @Test
    void updateProblemRejectsForeignAssignmentAsOwnershipViolation() {
        Long semesterId = createSemester("FP26");
        Long firstAssignmentId = createAssignment(semesterId, "First Owner");
        Long secondAssignmentId = createAssignment(semesterId, "Second Owner");
        Long foreignProblemId = problemService.create(problemForm(secondAssignmentId, "Foreign Question"));

        ProblemForm updateForm = problemForm(firstAssignmentId, "Moved Question");

        assertThatThrownBy(() -> problemService.update(foreignProblemId, updateForm))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Problem does not belong to this assignment.");
    }

    @Test
    void deleteProblemRejectsForeignAssignmentAsOwnershipViolation() {
        Long semesterId = createSemester("FDP26");
        Long firstAssignmentId = createAssignment(semesterId, "First Delete Owner");
        Long secondAssignmentId = createAssignment(semesterId, "Second Delete Owner");
        Long foreignProblemId = problemService.create(problemForm(secondAssignmentId, "Foreign Delete Question"));

        assertThatThrownBy(() -> problemService.delete(firstAssignmentId, foreignProblemId))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Problem does not belong to this assignment.");
    }

    private Long createSemester(String code) {
        Semester semester = new Semester();
        semester.setCode(code);
        semester.setName("Semester " + code);
        semester.setStartDate(LocalDate.of(2026, 1, 1));
        semester.setEndDate(LocalDate.of(2026, 5, 31));
        semester.setArchived(false);
        return semesterRepository.save(semester).getId();
    }

    private Long createAssignment(Long semesterId, String assignmentName) {
        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName(assignmentName);
        form.setSemesterId(semesterId);
        form.setGradingMode(GradingMode.JAVA_CORE);
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy(OutputNormalizationPolicy.STRICT);
        return assignmentService.create(form);
    }

    private ProblemForm problemForm(Long assignmentId, String title) {
        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle(title);
        problemForm.setMaxScore(java.math.BigDecimal.TEN);
        problemForm.setInputMode(InputMode.STDIN);
        problemForm.setOutputComparisonMode(OutputComparisonMode.EXACT);
        return problemForm;
    }
}
