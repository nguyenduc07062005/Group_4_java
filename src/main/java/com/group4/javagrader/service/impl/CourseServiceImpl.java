package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.ProblemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CourseServiceImpl implements CourseService {

    private static final String GENERAL_COURSE_CODE = "GENERAL";
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final AssignmentRepository assignmentRepository;
    private final ProblemService problemService;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            SemesterRepository semesterRepository,
            AssignmentRepository assignmentRepository,
            ProblemService problemService) {
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.assignmentRepository = assignmentRepository;
        this.problemService = problemService;
    }

    @Override
    @Transactional
    public Long create(CourseForm form) {
        Semester semester = resolveSemester(form.getSemesterId());
        String normalizedCode = normalizeCourseCode(form.getCourseCode());
        if (courseRepository.findBySemesterIdAndCourseCodeIgnoreCase(semester.getId(), normalizedCode).isPresent()) {
            throw new AssignmentConfigValidationException("courseCode", "Course code already exists in this semester.");
        }

        Course course = new Course();
        course.setSemester(semester);
        course.setCourseCode(normalizedCode);
        course.setCourseName(StringUtils.trimWhitespace(form.getCourseName()));
        course.setWeekCount(form.getWeekCount());
        course.setArchived(false);
        Course savedCourse = courseRepository.save(course);

        seedAssignments(savedCourse, form);
        return savedCourse.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findBySemesterId(Long semesterId) {
        return courseRepository.findBySemesterIdAndArchivedFalseOrderByCourseCodeAscIdAsc(semesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findBySemesterIds(List<Long> semesterIds) {
        if (semesterIds == null || semesterIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findBySemesterIdInAndArchivedFalseOrderBySemesterIdAscCourseCodeAscIdAsc(semesterIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id).filter(course -> !course.isArchived());
    }

    @Override
    @Transactional
    public Course ensureGeneralCourse(Long semesterId) {
        Semester semester = resolveSemester(semesterId);
        return courseRepository.findBySemesterIdAndCourseCodeIgnoreCase(semesterId, GENERAL_COURSE_CODE)
                .map(existingCourse -> {
                    if (!existingCourse.isArchived()) {
                        return existingCourse;
                    }
                    existingCourse.setArchived(false);
                    existingCourse.setCourseName("Mon hoc chung");
                    existingCourse.setWeekCount(0);
                    return courseRepository.save(existingCourse);
                })
                .orElseGet(() -> {
                    Course course = new Course();
                    course.setSemester(semester);
                    course.setCourseCode(GENERAL_COURSE_CODE);
                    course.setCourseName("Mon hoc chung");
                    course.setWeekCount(0);
                    course.setArchived(false);
                    return courseRepository.save(course);
                });
    }

    @Override
    @Transactional
    public void update(Long id, CourseForm form) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (course.isArchived()) {
            throw new WorkflowStateException("Cannot edit an archived course.");
        }

        Semester semester = resolveSemester(form.getSemesterId());
        if (!course.getSemester().getId().equals(semester.getId()) && assignmentRepository.existsByCourseId(id)) {
            throw new AssignmentConfigValidationException(
                    "semesterId",
                    "Cannot move a course with assignments to another semester.");
        }

        String normalizedCode = normalizeCourseCode(form.getCourseCode());
        if (courseRepository.existsBySemesterIdAndCourseCodeIgnoreCaseAndIdNot(
                semester.getId(), normalizedCode, id)) {
            throw new AssignmentConfigValidationException("courseCode", "Course code already exists in this semester.");
        }

        course.setSemester(semester);
        course.setCourseCode(normalizedCode);
        course.setCourseName(StringUtils.trimWhitespace(form.getCourseName()));
        course.setWeekCount(form.getWeekCount());

        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void archive(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (course.isArchived()) {
            return;
        }
        if (assignmentRepository.existsByCourseId(id)) {
            throw new WorkflowStateException("Cannot archive a course that still has assignments.");
        }
        course.setArchived(true);
        courseRepository.save(course);
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId == null) {
            throw new AssignmentConfigValidationException("semesterId", "Semester must be selected.");
        }

        return semesterRepository.findByIdAndArchivedFalseForUpdate(semesterId)
                .orElseThrow(() -> new AssignmentConfigValidationException("semesterId", "Selected semester was not found."));
    }

    private void seedAssignments(Course course, CourseForm form) {
        if (form.isCreateWeeklyAssignments()) {
            for (int weekNumber = 1; weekNumber <= form.getWeekCount(); weekNumber++) {
                createSeedAssignment(course, "Tuan " + weekNumber, AssignmentType.WEEKLY, weekNumber);
            }
        }
    }

    private void createSeedAssignment(
            Course course,
            String assignmentName,
            AssignmentType assignmentType,
            Integer weekNumber) {
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setSemester(course.getSemester());
        assignment.setAssignmentName(assignmentName);
        assignment.setAssignmentType(assignmentType);
        assignment.setWeekNumber(weekNumber);
        assignment.setDisplayOrder(weekNumber != null ? 100 + weekNumber : 1000);
        assignment.setGradingMode(GradingMode.JAVA_CORE);
        assignment.setPlagiarismThreshold(BigDecimal.valueOf(80));
        assignment.setOutputNormalizationPolicy(OutputNormalizationPolicy.STRICT);
        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        problemService.ensureDefaultRuntimeProblem(
                savedAssignment.getId(),
                InputMode.STDIN,
                OutputNormalizationPolicy.STRICT);
    }

    private String normalizeCourseCode(String courseCode) {
        return courseCode.trim().toUpperCase(Locale.ROOT);
    }
}
