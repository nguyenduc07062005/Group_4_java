package com.group4.javagrader.course;

import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CourseServiceIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Test
    void createCourseSeedsOnlyWeeklyAssignments() {
        Long semesterId = createSemester("SWE26");

        CourseForm form = new CourseForm();
        form.setSemesterId(semesterId);
        form.setCourseCode("SWE201");
        form.setCourseName("Software Engineering");
        form.setWeekCount(3);
        form.setCreateWeeklyAssignments(true);

        Long courseId = courseService.create(form);

        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Assignment> assignments = assignmentRepository.findByCourseIdOrderByDisplayOrderAscIdAsc(courseId);

        assertThat(course.getSemester().getId()).isEqualTo(semesterId);
        assertThat(course.getCourseCode()).isEqualTo("SWE201");
        assertThat(assignments).hasSize(3);
        assertThat(assignments).extracting(Assignment::getAssignmentType)
                .containsExactly(AssignmentType.WEEKLY, AssignmentType.WEEKLY, AssignmentType.WEEKLY);
        assertThat(assignments).extracting(Assignment::getAssignmentName)
                .containsExactly("Tuan 1", "Tuan 2", "Tuan 3");
        assertThat(assignments).extracting(Assignment::getWeekNumber)
                .containsExactly(1, 2, 3);
    }

    @Test
    void archiveCourseRejectsCoursesThatStillHaveAssignments() {
        Long semesterId = createSemester("SWE27");

        CourseForm form = new CourseForm();
        form.setSemesterId(semesterId);
        form.setCourseCode("SWE202");
        form.setCourseName("Software Testing");
        form.setWeekCount(2);
        form.setCreateWeeklyAssignments(true);

        Long courseId = courseService.create(form);

        assertThatThrownBy(() -> courseService.archive(courseId))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Cannot archive a course that still has assignments.");

        Course course = courseRepository.findById(courseId).orElseThrow();
        assertThat(course.isArchived()).isFalse();
    }

    @Test
    void updateRejectsMovingCourseToAnotherSemesterWhenAssignmentsAlreadyExist() {
        Long originalSemesterId = createSemester("SWE28");
        Long targetSemesterId = createSemester("SWE29");

        CourseForm createForm = new CourseForm();
        createForm.setSemesterId(originalSemesterId);
        createForm.setCourseCode("SWE203");
        createForm.setCourseName("Software Project");
        createForm.setWeekCount(2);
        createForm.setCreateWeeklyAssignments(true);

        Long courseId = courseService.create(createForm);

        CourseForm updateForm = new CourseForm();
        updateForm.setSemesterId(targetSemesterId);
        updateForm.setCourseCode("SWE203");
        updateForm.setCourseName("Software Project");
        updateForm.setWeekCount(2);
        updateForm.setCreateWeeklyAssignments(false);

        assertThatThrownBy(() -> courseService.update(courseId, updateForm))
                .isInstanceOf(AssignmentConfigValidationException.class)
                .hasMessage("Cannot move a course with assignments to another semester.");

        Course course = courseRepository.findById(courseId).orElseThrow();
        assertThat(course.getSemester().getId()).isEqualTo(originalSemesterId);
        assertThat(assignmentRepository.findByCourseIdOrderByDisplayOrderAscIdAsc(courseId))
                .allMatch(assignment -> assignment.getSemester().getId().equals(originalSemesterId));
    }

    @Test
    void createRejectsCodeUsedByArchivedCourseInSameSemester() {
        Long semesterId = createSemester("SWE30");

        CourseForm firstForm = new CourseForm();
        firstForm.setSemesterId(semesterId);
        firstForm.setCourseCode("SWE204");
        firstForm.setCourseName("Intro to QA");
        firstForm.setWeekCount(0);
        firstForm.setCreateWeeklyAssignments(false);

        Long courseId = courseService.create(firstForm);
        courseService.archive(courseId);

        CourseForm secondForm = new CourseForm();
        secondForm.setSemesterId(semesterId);
        secondForm.setCourseCode("SWE204");
        secondForm.setCourseName("Intro to QA Reloaded");
        secondForm.setWeekCount(0);
        secondForm.setCreateWeeklyAssignments(false);

        assertThatThrownBy(() -> courseService.create(secondForm))
                .isInstanceOf(AssignmentConfigValidationException.class)
                .hasMessage("Course code already exists in this semester.");
    }

    @Test
    void ensureGeneralCourseRevivesArchivedFallbackCourse() {
        Long semesterId = createSemester("SWE31");

        Course generalCourse = courseService.ensureGeneralCourse(semesterId);
        courseService.archive(generalCourse.getId());

        Course revivedCourse = courseService.ensureGeneralCourse(semesterId);

        assertThat(revivedCourse.getId()).isEqualTo(generalCourse.getId());
        assertThat(revivedCourse.isArchived()).isFalse();
        assertThat(revivedCourse.getCourseCode()).isEqualTo("GENERAL");
        assertThat(courseRepository.findAll().stream()
                .filter(course -> course.getSemester().getId().equals(semesterId))
                .filter(course -> "GENERAL".equals(course.getCourseCode())))
                .hasSize(1);
    }

    @Test
    void updateRejectsMissingCourseAsResourceNotFound() {
        CourseForm form = courseForm(createSemester("SWE32"), "SWE205", "Missing Course");

        assertThatThrownBy(() -> courseService.update(999_999L, form))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found.");
    }

    @Test
    void archiveRejectsMissingCourseAsResourceNotFound() {
        assertThatThrownBy(() -> courseService.archive(999_998L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found.");
    }

    @Test
    void updateRejectsArchivedCourseAsWorkflowState() {
        Long semesterId = createSemester("SWE33");
        Long courseId = courseService.create(courseForm(semesterId, "SWE206", "Archived Course"));
        courseService.archive(courseId);

        CourseForm updateForm = courseForm(semesterId, "SWE207", "Archived Course Updated");

        assertThatThrownBy(() -> courseService.update(courseId, updateForm))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Cannot edit an archived course.");
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

    private CourseForm courseForm(Long semesterId, String courseCode, String courseName) {
        CourseForm form = new CourseForm();
        form.setSemesterId(semesterId);
        form.setCourseCode(courseCode);
        form.setCourseName(courseName);
        form.setWeekCount(0);
        form.setCreateWeeklyAssignments(false);
        return form;
    }
}
