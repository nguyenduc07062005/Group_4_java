package com.group4.javagrader.submission;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmissionRepositoryIntegrationTest {

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void summarizeByAssignmentIdsUsesLatestSubmissionUpdateTimestamp() {
        Assignment assignment = createAssignment();
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setSubmitterName("s2213001");
        submission.setArchiveFileName("first.zip");
        submission.setStatus(SubmissionStatus.VALIDATED);
        submission.setValidationMessage("All validation checks passed.");
        submission.setFileCount(1);
        submission = submissionRepository.saveAndFlush(submission);
        entityManager.refresh(submission);

        LocalDateTime reuploadAt = submission.getCreatedAt().plusMinutes(10);
        submission.setArchiveFileName("second.zip");
        submission.setUpdatedAt(reuploadAt);
        submissionRepository.saveAndFlush(submission);
        entityManager.clear();

        List<SubmissionRepository.AssignmentSubmissionStats> stats =
                submissionRepository.summarizeByAssignmentIds(List.of(assignment.getId()));

        assertThat(stats)
                .singleElement()
                .satisfies(row -> assertThat(row.getLatestSubmissionAt()).isEqualTo(reuploadAt));
    }

    @Test
    void duplicateSubmitterRowsAreRejectedWithinOneAssignment() {
        Assignment assignment = createAssignment();
        submissionRepository.saveAndFlush(submission(assignment, "s2213001", "first.zip"));

        assertThatThrownBy(() -> submissionRepository.saveAndFlush(submission(assignment, "s2213001", "second.zip")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private Assignment createAssignment() {
        Semester semester = new Semester();
        semester.setCode("SP26-REPO-" + System.nanoTime());
        semester.setName("Spring 2026");
        semester.setStartDate(LocalDate.of(2026, 1, 6));
        semester.setEndDate(LocalDate.of(2026, 5, 30));
        semester = semesterRepository.saveAndFlush(semester);

        Course course = new Course();
        course.setSemester(semester);
        course.setCourseCode("PRJ301");
        course.setCourseName("Java Project");
        course.setWeekCount(12);
        course = courseRepository.saveAndFlush(course);

        Assignment assignment = new Assignment();
        assignment.setSemester(semester);
        assignment.setCourse(course);
        assignment.setAssignmentName("Repository Timestamp Lab");
        assignment.setAssignmentType(com.group4.javagrader.entity.AssignmentType.CUSTOM);
        assignment.setDisplayOrder(1000);
        assignment.setGradingMode(GradingMode.JAVA_CORE);
        assignment.setPlagiarismThreshold(BigDecimal.valueOf(80));
        assignment.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);
        return assignmentRepository.saveAndFlush(assignment);
    }

    private Submission submission(Assignment assignment, String submitterName, String archiveFileName) {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setSubmitterName(submitterName);
        submission.setArchiveFileName(archiveFileName);
        submission.setStatus(SubmissionStatus.VALIDATED);
        submission.setValidationMessage("All validation checks passed.");
        submission.setFileCount(1);
        return submission;
    }
}
