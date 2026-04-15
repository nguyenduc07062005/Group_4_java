package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.AssignmentWorkspaceForm;
import com.group4.javagrader.dto.ProblemForm;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.dto.WorkspaceProblemForm;
import com.group4.javagrader.dto.WorkspaceTestCaseForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentAttachment;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import com.group4.javagrader.entity.AssignmentType;
import com.group4.javagrader.entity.Course;
import com.group4.javagrader.entity.GradingMode;
import com.group4.javagrader.entity.InputMode;
import com.group4.javagrader.entity.OutputNormalizationPolicy;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.AssignmentAttachmentRepository;
import com.group4.javagrader.repository.BatchRepository;
import com.group4.javagrader.repository.CourseRepository;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.service.AssignmentService;
import com.group4.javagrader.service.CourseService;
import com.group4.javagrader.service.ProblemService;
import com.group4.javagrader.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private static final Set<String> DESCRIPTION_EXTENSIONS = Set.of("pdf", "md");
    private static final Set<String> OOP_RULE_EXTENSIONS = Set.of("json");
    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final BatchRepository batchRepository;
    private final CourseService courseService;
    private final ProblemService problemService;
    private final TestCaseService testCaseService;

    public AssignmentServiceImpl(
            AssignmentRepository assignmentRepository,
            AssignmentAttachmentRepository assignmentAttachmentRepository,
            SemesterRepository semesterRepository,
            CourseRepository courseRepository,
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            SubmissionRepository submissionRepository,
            BatchRepository batchRepository,
            CourseService courseService,
            ProblemService problemService,
            TestCaseService testCaseService) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentAttachmentRepository = assignmentAttachmentRepository;
        this.semesterRepository = semesterRepository;
        this.courseRepository = courseRepository;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.batchRepository = batchRepository;
        this.courseService = courseService;
        this.problemService = problemService;
        this.testCaseService = testCaseService;
    }

    @Override
    @Transactional
    public Long create(AssignmentForm form) {
        Assignment assignment = new Assignment();
        applyForm(assignment, form, false);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        persistUploads(savedAssignment, form, false);
        problemService.ensureDefaultRuntimeProblem(
                savedAssignment.getId(),
                form.getInputMode(),
                form.getOutputNormalizationPolicy());
        return savedAssignment.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AssignmentForm form) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        applyForm(assignment, form, true);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        persistUploads(savedAssignment, form, true);
        problemService.ensureDefaultRuntimeProblem(
                savedAssignment.getId(),
                form.getInputMode(),
                form.getOutputNormalizationPolicy());
    }

    @Override
    @Transactional
    public Long saveWorkspace(AssignmentWorkspaceForm form) {
        Long assignmentId = form.getId();
        if (assignmentId == null) {
            assignmentId = create(form);
        } else {
            update(assignmentId, form);
        }

        for (WorkspaceProblemForm workspaceProblem : form.getProblems()) {
            Long problemId = saveWorkspaceProblem(assignmentId, workspaceProblem);
            for (WorkspaceTestCaseForm workspaceTestCase : workspaceProblem.getTestCases()) {
                saveWorkspaceTestCase(problemId, workspaceTestCase);
            }
        }
        return assignmentId;
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentWorkspaceForm loadWorkspace(Long id) {
        Assignment assignment = assignmentRepository.findByIdWithSemesterAndCourse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        AssignmentWorkspaceForm form = new AssignmentWorkspaceForm();
        form.setId(assignment.getId());
        form.setAssignmentName(assignment.getAssignmentName());
        form.setSemesterId(assignment.getSemester() != null ? assignment.getSemester().getId() : null);
        form.setCourseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null);
        form.setGradingMode(assignment.getGradingMode());
        form.setAssignmentType(assignment.getAssignmentType());
        form.setWeekNumber(assignment.getWeekNumber());
        form.setPlagiarismThreshold(assignment.getPlagiarismThreshold().intValue());
        form.setOutputNormalizationPolicy(assignment.getOutputNormalizationPolicy());
        form.setLogicWeight(assignment.getLogicWeight());
        form.setOopWeight(assignment.getOopWeight());
        form.setInputMode(InputMode.STDIN);

        List<Problem> problems = problemRepository.findByAssignmentIdOrderByProblemOrderAsc(id);
        List<WorkspaceProblemForm> workspaceProblems = new ArrayList<>();

        for (Problem problem : problems) {
            if (problem.isInternalDefault()) {
                continue;
            }
            form.setInputMode(problem.getInputMode());
            workspaceProblems.add(toWorkspaceProblemForm(problem));
        }

        form.setProblems(workspaceProblems);
        return form;
    }

    private Long saveWorkspaceProblem(Long assignmentId, WorkspaceProblemForm workspaceProblem) {
        if (workspaceProblem.getId() != null) {
            Problem existingProblem = loadWorkspaceProblemOrThrow(assignmentId, workspaceProblem.getId());
            applyWorkspaceProblem(existingProblem, workspaceProblem);
            return problemRepository.save(existingProblem).getId();
        }

        ProblemForm problemForm = new ProblemForm();
        problemForm.setAssignmentId(assignmentId);
        problemForm.setTitle(workspaceProblem.getTitle());
        problemForm.setMaxScore(workspaceProblem.getMaxScore());
        problemForm.setInputMode(workspaceProblem.getInputMode());
        problemForm.setOutputComparisonMode(workspaceProblem.getOutputComparisonMode());
        return problemService.create(problemForm);
    }

    private void saveWorkspaceTestCase(Long problemId, WorkspaceTestCaseForm workspaceTestCase) {
        if (workspaceTestCase.getId() != null) {
            TestCase existingTestCase = loadWorkspaceTestCaseOrThrow(problemId, workspaceTestCase.getId());
            applyWorkspaceTestCase(existingTestCase, workspaceTestCase);
            testCaseRepository.save(existingTestCase);
            return;
        }

        TestCaseForm testCaseForm = new TestCaseForm();
        testCaseForm.setProblemId(problemId);
        testCaseForm.setInputData(workspaceTestCase.getInputData());
        testCaseForm.setExpectedOutput(workspaceTestCase.getExpectedOutput());
        testCaseForm.setWeight(workspaceTestCase.getWeight());
        testCaseService.create(testCaseForm);
    }

    private WorkspaceProblemForm toWorkspaceProblemForm(Problem problem) {
        WorkspaceProblemForm workspaceProblem = new WorkspaceProblemForm();
        workspaceProblem.setId(problem.getId());
        workspaceProblem.setTitle(problem.getTitle());
        workspaceProblem.setMaxScore(problem.getMaxScore());
        workspaceProblem.setInputMode(problem.getInputMode());
        workspaceProblem.setOutputComparisonMode(problem.getOutputComparisonMode());
        workspaceProblem.setTestCases(testCaseService.findByProblemId(problem.getId()).stream()
                .map(this::toWorkspaceTestCaseForm)
                .toList());
        return workspaceProblem;
    }

    private WorkspaceTestCaseForm toWorkspaceTestCaseForm(TestCase testCase) {
        WorkspaceTestCaseForm workspaceTestCase = new WorkspaceTestCaseForm();
        workspaceTestCase.setId(testCase.getId());
        workspaceTestCase.setInputData(testCase.getInputData());
        workspaceTestCase.setExpectedOutput(testCase.getExpectedOutput());
        workspaceTestCase.setWeight(testCase.getWeight());
        return workspaceTestCase;
    }

    private Problem loadWorkspaceProblemOrThrow(Long assignmentId, Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found."));
        if (!problem.getAssignment().getId().equals(assignmentId)) {
            throw new OwnershipViolationException("Problem does not belong to this assignment.");
        }
        return problem;
    }

    private void applyWorkspaceProblem(Problem problem, WorkspaceProblemForm form) {
        problem.setTitle(form.getTitle().trim());
        problem.setMaxScore(form.getMaxScore());
        problem.setInputMode(form.getInputMode());
        problem.setOutputComparisonMode(form.getOutputComparisonMode());
    }

    private TestCase loadWorkspaceTestCaseOrThrow(Long problemId, Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Testcase not found."));
        if (!testCase.getProblem().getId().equals(problemId)) {
            throw new OwnershipViolationException("Testcase does not belong to this problem.");
        }
        return testCase;
    }

    private void applyWorkspaceTestCase(TestCase testCase, WorkspaceTestCaseForm form) {
        testCase.setInputData(hasText(form.getInputData()) ? normalizeText(form.getInputData()) : null);
        testCase.setExpectedOutput(normalizeText(form.getExpectedOutput()));
        testCase.setWeight(form.getWeight());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        if (!canDelete(id)) {
            throw new WorkflowStateException("Cannot delete assignment after setup has started.");
        }

        problemRepository.deleteAll(problemRepository.findByAssignmentIdOrderByProblemOrderAsc(id));
        assignmentRepository.delete(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete(Long id) {
        boolean hasVisibleProblems = problemRepository.existsByAssignmentIdAndInternalDefaultFalse(id);
        boolean hasDefaultTestcases = problemRepository.findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(id)
                .map(Problem::getId)
                .map(testCaseRepository::countByProblemId)
                .orElse(0L) > 0;
        return !hasVisibleProblems
                && !hasDefaultTestcases
                && !submissionRepository.existsByAssignmentId(id)
                && !batchRepository.existsByAssignmentId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assignment> findById(Long id) {
        return assignmentRepository.findByIdWithSemesterAndCourse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findAttachmentData(Long assignmentId, AssignmentAttachmentType attachmentType) {
        return assignmentAttachmentRepository.findByAssignmentIdAndAttachmentType(assignmentId, attachmentType)
                .map(AssignmentAttachment::getData)
                .filter(data -> data != null && data.length > 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findBySemesterId(Long semesterId) {
        return assignmentRepository.findBySemesterIdOrderByCourseIdAscDisplayOrderAscIdAsc(semesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findBySemesterIds(List<Long> semesterIds) {
        if (semesterIds == null || semesterIds.isEmpty()) {
            return List.of();
        }
        return assignmentRepository.findBySemesterIdInOrderBySemesterIdAscCourseIdAscDisplayOrderAscIdAsc(semesterIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> findByCourseId(Long courseId) {
        return assignmentRepository.findByCourseIdOrderByDisplayOrderAscIdAsc(courseId);
    }

    private void applyForm(Assignment assignment, AssignmentForm form, boolean preserveExistingUploads) {
        Course course = resolveTargetCourse(form);
        Semester semester = course.getSemester();
        AssignmentType assignmentType = normalizeAssignmentType(form.getAssignmentType());
        Integer normalizedWeekNumber = normalizeWeekNumber(assignmentType, form.getWeekNumber());

        assignment.setCourse(course);
        assignment.setSemester(semester);
        assignment.setAssignmentName(form.getAssignmentName().trim());
        assignment.setAssignmentType(assignmentType);
        assignment.setWeekNumber(normalizedWeekNumber);
        assignment.setDisplayOrder(resolveDisplayOrder(assignmentType, normalizedWeekNumber));
        assignment.setGradingMode(form.getGradingMode());
        assignment.setPlagiarismThreshold(BigDecimal.valueOf(form.getPlagiarismThreshold()));
        assignment.setOutputNormalizationPolicy(form.getOutputNormalizationPolicy());

        applyDescriptionUpload(form.getDescriptionFile(), assignment, preserveExistingUploads);
        applyModeSpecificConfiguration(form, assignment, preserveExistingUploads);
    }

    private Course resolveTargetCourse(AssignmentForm form) {
        if (form.getCourseId() != null) {
            Course course = courseRepository.findById(form.getCourseId())
                    .filter(foundCourse -> !foundCourse.isArchived())
                    .orElseThrow(() -> new AssignmentConfigValidationException("courseId", "Selected course was not found."));

            if (form.getSemesterId() != null && !course.getSemester().getId().equals(form.getSemesterId())) {
                throw new AssignmentConfigValidationException("courseId", "Selected course does not belong to the selected semester.");
            }
            return course;
        }

        Semester semester = resolveTargetSemester(form.getSemesterId());
        return courseService.ensureGeneralCourse(semester.getId());
    }

    private Semester resolveTargetSemester(Long semesterId) {
        if (semesterId == null) {
            throw new AssignmentConfigValidationException("semesterId", "Semester must be selected.");
        }

        return semesterRepository.findByIdAndArchivedFalseForUpdate(semesterId)
                .orElseThrow(() -> new AssignmentConfigValidationException("semesterId", "Selected semester was not found."));
    }

    private AssignmentType normalizeAssignmentType(AssignmentType assignmentType) {
        return assignmentType == null ? AssignmentType.CUSTOM : assignmentType;
    }

    private Integer normalizeWeekNumber(AssignmentType assignmentType, Integer weekNumber) {
        if (assignmentType != AssignmentType.WEEKLY) {
            return null;
        }
        if (weekNumber == null || weekNumber < 1) {
            throw new AssignmentConfigValidationException("weekNumber", "Week number is required for weekly assignments.");
        }
        return weekNumber;
    }

    private int resolveDisplayOrder(AssignmentType assignmentType, Integer weekNumber) {
        return switch (assignmentType) {
            case INTRO -> 0;
            case DEFAULT -> 10;
            case WEEKLY -> 100 + weekNumber;
            default -> 1000;
        };
    }

    private void applyDescriptionUpload(MultipartFile file, Assignment assignment, boolean preserveExistingUpload) {
        if (!hasUploadedFile(file)) {
            if (!preserveExistingUpload) {
                assignment.setDescriptionFileName(null);
                assignment.setDescriptionFileContentType(null);
                assignment.setDescription(null);
            }
            return;
        }

        String fileName = normalizeFileName(file, "descriptionFile");
        validateFileExtension(
                fileName,
                DESCRIPTION_EXTENSIONS,
                "descriptionFile",
                "Assignment description must be a .pdf or .md file.");

        byte[] fileBytes = readFileBytes(file, "descriptionFile");
        assignment.setDescriptionFileName(fileName);
        assignment.setDescriptionFileContentType(emptyToNull(file.getContentType()));

        if (fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            assignment.setDescription(new String(fileBytes, StandardCharsets.UTF_8));
            return;
        }

        assignment.setDescription(null);
    }

    private void applyModeSpecificConfiguration(
            AssignmentForm form,
            Assignment assignment,
            boolean preserveExistingUpload) {
        if (form.getGradingMode() == GradingMode.OOP) {
            assignment.setLogicWeight(form.getLogicWeight());
            assignment.setOopWeight(form.getOopWeight());
            applyOopRuleUpload(form.getOopRuleConfig(), assignment, preserveExistingUpload);
            return;
        }

        assignment.setLogicWeight(100);
        assignment.setOopWeight(0);
        assignment.setOopRuleConfigFileName(null);
        assignment.setOopRuleConfigContentType(null);
    }

    private void applyOopRuleUpload(
            MultipartFile file,
            Assignment assignment,
            boolean preserveExistingUpload) {
        if (!hasUploadedFile(file)) {
            if (!preserveExistingUpload) {
                assignment.setOopRuleConfigFileName(null);
                assignment.setOopRuleConfigContentType(null);
            }
            return;
        }

        String fileName = normalizeFileName(file, "oopRuleConfig");
        validateFileExtension(
                fileName,
                OOP_RULE_EXTENSIONS,
                "oopRuleConfig",
                "OOP rule configuration must be a .json file.");

        assignment.setOopRuleConfigFileName(fileName);
        assignment.setOopRuleConfigContentType(emptyToNull(file.getContentType()));
    }

    private void persistUploads(Assignment assignment, AssignmentForm form, boolean preserveExistingUploads) {
        persistDescriptionUpload(assignment, form.getDescriptionFile(), preserveExistingUploads);
        persistOopRuleUpload(assignment, form.getOopRuleConfig(), preserveExistingUploads, form.getGradingMode());
    }

    private void persistDescriptionUpload(
            Assignment assignment,
            MultipartFile file,
            boolean preserveExistingUpload) {
        if (!hasUploadedFile(file)) {
            if (!preserveExistingUpload) {
                assignmentAttachmentRepository.deleteByAssignmentIdAndAttachmentType(
                        assignment.getId(),
                        AssignmentAttachmentType.DESCRIPTION);
            }
            return;
        }

        String fileName = normalizeFileName(file, "descriptionFile");
        validateFileExtension(
                fileName,
                DESCRIPTION_EXTENSIONS,
                "descriptionFile",
                "Assignment description must be a .pdf or .md file.");
        upsertAttachment(
                assignment,
                AssignmentAttachmentType.DESCRIPTION,
                fileName,
                emptyToNull(file.getContentType()),
                readFileBytes(file, "descriptionFile"));
    }

    private void persistOopRuleUpload(
            Assignment assignment,
            MultipartFile file,
            boolean preserveExistingUpload,
            GradingMode gradingMode) {
        if (gradingMode != GradingMode.OOP) {
            assignmentAttachmentRepository.deleteByAssignmentIdAndAttachmentType(
                    assignment.getId(),
                    AssignmentAttachmentType.OOP_RULE_CONFIG);
            return;
        }

        if (!hasUploadedFile(file)) {
            if (!preserveExistingUpload) {
                assignmentAttachmentRepository.deleteByAssignmentIdAndAttachmentType(
                        assignment.getId(),
                        AssignmentAttachmentType.OOP_RULE_CONFIG);
            }
            return;
        }

        String fileName = normalizeFileName(file, "oopRuleConfig");
        validateFileExtension(
                fileName,
                OOP_RULE_EXTENSIONS,
                "oopRuleConfig",
                "OOP rule configuration must be a .json file.");
        upsertAttachment(
                assignment,
                AssignmentAttachmentType.OOP_RULE_CONFIG,
                fileName,
                emptyToNull(file.getContentType()),
                readFileBytes(file, "oopRuleConfig"));
    }

    private void upsertAttachment(
            Assignment assignment,
            AssignmentAttachmentType attachmentType,
            String fileName,
            String contentType,
            byte[] data) {
        AssignmentAttachment attachment = assignmentAttachmentRepository
                .findByAssignmentIdAndAttachmentType(assignment.getId(), attachmentType)
                .orElseGet(() -> {
                    AssignmentAttachment created = new AssignmentAttachment();
                    created.setAssignment(assignment);
                    created.setAttachmentType(attachmentType);
                    return created;
                });
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setData(data);
        assignmentAttachmentRepository.save(attachment);
    }

    private boolean hasUploadedFile(MultipartFile file) {
        return file != null
                && file.getOriginalFilename() != null
                && !file.getOriginalFilename().isBlank();
    }

    private String normalizeFileName(MultipartFile file, String fieldName) {
        if (file.isEmpty()) {
            throw new AssignmentConfigValidationException(fieldName, "Uploaded file must not be empty.");
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(fileName) || fileName.contains("..")) {
            throw new AssignmentConfigValidationException(fieldName, "Uploaded file name is invalid.");
        }

        return fileName;
    }

    private void validateFileExtension(
            String fileName,
            Set<String> allowedExtensions,
            String fieldName,
            String message) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new AssignmentConfigValidationException(fieldName, message);
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new AssignmentConfigValidationException(fieldName, message);
        }
    }

    private byte[] readFileBytes(MultipartFile file, String fieldName) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new AssignmentConfigValidationException(fieldName, "Uploaded file could not be read.");
        }
    }

    private String emptyToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value;
    }
}
