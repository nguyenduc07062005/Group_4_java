package com.group4.javagrader.phaseone;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.SemesterService;
import com.group4.javagrader.service.TestCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PhaseOneCreationFlowIntegrationTest {

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Test
    void createCompletePhaseOneChain() {
        SemesterForm semesterForm = new SemesterForm();
        semesterForm.setCode("SP26-P1");
        semesterForm.setName("Spring 2026");
        semesterForm.setStartDate(LocalDate.of(2026, 1, 6));
        semesterForm.setEndDate(LocalDate.of(2026, 5, 30));
        Long semesterId = semesterService.create(semesterForm);

        AssignmentForm assignmentForm = new AssignmentForm();
        assignmentForm.setAssignmentName("Lab 01");
        assignmentForm.setSemesterId(semesterId);
        assignmentForm.setGradingMode(com.group4.javagrader.entity.GradingMode.JAVA_CORE);
        assignmentForm.setPlagiarismThreshold(20);
        assignmentForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        Long assignmentId = assignmentService.create(assignmentForm);

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle("Sum of Two Numbers");
        problemForm.setMaxScore(BigDecimal.valueOf(10));
        problemForm.setInputMode(com.group4.javagrader.entity.InputMode.STDIN);
        problemForm.setOutputComparisonMode(com.group4.javagrader.entity.OutputComparisonMode.EXACT);
        Long problemId = problemService.create(problemForm);

        TestCaseForm testCaseForm = new TestCaseForm();
        testCaseForm.setProblemId(problemId);
        testCaseForm.setInputData("1 2");
        testCaseForm.setExpectedOutput("3");
        testCaseForm.setWeight(BigDecimal.ONE);
        Long testCaseId = testCaseService.create(testCaseForm);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        Problem problem = problemRepository.findById(problemId).orElseThrow();
        TestCase testCase = testCaseRepository.findById(testCaseId).orElseThrow();

        assertThat(assignment.getSemester().getId()).isEqualTo(semesterId);
        assertThat(problem.getAssignment().getId()).isEqualTo(assignmentId);
        assertThat(problem.getProblemOrder()).isEqualTo(1);
        assertThat(testCase.getProblem().getId()).isEqualTo(problemId);
        assertThat(testCase.getCaseOrder()).isEqualTo(1);
        assertThat(testCase.getWeight()).isEqualByComparingTo("1.00");
        assertThat(testCase.isSample()).isFalse();
    }
}
