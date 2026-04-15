package com.group4.javagrader.semester;

import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.dto.SemesterForm;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.SemesterService;
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
class SemesterServiceIntegrationTest {

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private CourseService courseService;

    @Test
    void findActiveSemestersUsesStableIdTieBreakerWhenStartDatesMatch() {
        String prefix = "TIE-" + System.nanoTime();
        Long olderSemesterId = createSemester(prefix + "-A");
        Long newerSemesterId = createSemester(prefix + "-B");

        List<Long> orderedIds = semesterService.findActiveSemesters().stream()
                .filter(semester -> semester.getCode().startsWith(prefix))
                .map(Semester::getId)
                .toList();

        assertThat(orderedIds).containsExactly(newerSemesterId, olderSemesterId);
    }

    @Test
    void createRejectsDuplicateCodeAsInputValidation() {
        String code = "DUP-" + System.nanoTime();
        createSemester(code);

        SemesterForm form = semesterForm(code);

        assertThatThrownBy(() -> semesterService.create(form))
                .isInstanceOf(InputValidationException.class)
                .hasMessage("Semester code already exists.");
    }

    @Test
    void updateRejectsMissingSemesterAsResourceNotFound() {
        SemesterForm form = semesterForm("MISS-" + System.nanoTime());

        assertThatThrownBy(() -> semesterService.update(999_999L, form))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Semester not found.");
    }

    @Test
    void archiveRejectsMissingSemesterAsResourceNotFound() {
        assertThatThrownBy(() -> semesterService.archive(999_998L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Semester not found.");
    }

    @Test
    void archiveRejectsSemesterWithActiveCoursesAsWorkflowState() {
        Long semesterId = createSemester("LOCK-" + System.nanoTime());
        CourseForm courseForm = new CourseForm();
        courseForm.setSemesterId(semesterId);
        courseForm.setCourseCode("SWE400");
        courseForm.setCourseName("Workflow Guard");
        courseForm.setWeekCount(0);
        courseForm.setCreateWeeklyAssignments(false);
        courseService.create(courseForm);

        assertThatThrownBy(() -> semesterService.archive(semesterId))
                .isInstanceOf(WorkflowStateException.class)
                .hasMessage("Cannot archive a semester that still has active courses or assignments.");
    }

    private Long createSemester(String code) {
        return semesterService.create(semesterForm(code));
    }

    private SemesterForm semesterForm(String code) {
        SemesterForm form = new SemesterForm();
        form.setCode(code);
        form.setName("Semester " + code);
        form.setStartDate(LocalDate.of(2026, 1, 6));
        form.setEndDate(LocalDate.of(2026, 5, 30));
        return form;
    }
}
