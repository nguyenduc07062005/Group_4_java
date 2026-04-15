package com.group4.javagrader.assignment;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentAttachment;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.grading.oop.OopRuleChecker;
import com.group4.javagrader.grading.oop.OopRuleCheckerFactory;
import com.group4.javagrader.repository.AssignmentAttachmentRepository;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AssignmentAttachmentIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentAttachmentRepository assignmentAttachmentRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private OopRuleCheckerFactory oopRuleCheckerFactory;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void createStoresUploadedAttachmentsAndDownloadEndpointsReturnStoredBytes() throws Exception {
        Long semesterId = createSemester("AT26");

        byte[] descriptionBytes = "# Attachment Brief".getBytes(StandardCharsets.UTF_8);
        byte[] oopRuleBytes = """
                {"rules":[{"type":"minimum_class_count","value":3,"label":"At least three classes"}]}
                """.trim().getBytes(StandardCharsets.UTF_8);

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Attachment Lab");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        form.setPlagiarismThreshold(25);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        form.setLogicWeight(60);
        form.setOopWeight(40);
        form.setDescriptionFile(new MockMultipartFile(
                "descriptionFile",
                "brief.md",
                "text/markdown",
                descriptionBytes));
        form.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                oopRuleBytes));

        Long assignmentId = assignmentService.create(form);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(assignment.getDescriptionFileName()).isEqualTo("brief.md");
        assertThat(assignment.getOopRuleConfigFileName()).isEqualTo("rules.json");
        assertThat(assignment.getDescription()).contains("Attachment Brief");

        AssignmentAttachment descriptionAttachment = assignmentAttachmentRepository
                .findByAssignmentIdAndAttachmentType(assignmentId, AssignmentAttachmentType.DESCRIPTION)
                .orElseThrow();
        assertThat(descriptionAttachment.getFileName()).isEqualTo("brief.md");
        assertThat(descriptionAttachment.getContentType()).isEqualTo("text/markdown");
        assertThat(descriptionAttachment.getData()).isEqualTo(descriptionBytes);

        AssignmentAttachment oopRuleAttachment = assignmentAttachmentRepository
                .findByAssignmentIdAndAttachmentType(assignmentId, AssignmentAttachmentType.OOP_RULE_CONFIG)
                .orElseThrow();
        assertThat(oopRuleAttachment.getFileName()).isEqualTo("rules.json");
        assertThat(oopRuleAttachment.getContentType()).isEqualTo("application/json");
        assertThat(oopRuleAttachment.getData()).isEqualTo(oopRuleBytes);

        mockMvc.perform(get("/assignments/{id}/description-file", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Assignment-Id", String.valueOf(assignmentId)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("brief.md")))
                .andExpect(content().bytes(descriptionBytes));

        mockMvc.perform(get("/assignments/{id}/oop-rule-config", assignmentId)
                        .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Assignment-Id", String.valueOf(assignmentId)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("rules.json")))
                .andExpect(content().bytes(oopRuleBytes));
    }

    @Test
    void updateWithoutNewUploadsPreservesAttachmentRows() {
        Long semesterId = createSemester("AT27");

        AssignmentForm createForm = new AssignmentForm();
        createForm.setAssignmentName("Persistent Lab");
        createForm.setSemesterId(semesterId);
        createForm.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        createForm.setPlagiarismThreshold(25);
        createForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        createForm.setLogicWeight(60);
        createForm.setOopWeight(40);
        createForm.setDescriptionFile(new MockMultipartFile(
                "descriptionFile",
                "brief.md",
                "text/markdown",
                "# Persistent Lab".getBytes(StandardCharsets.UTF_8)));
        createForm.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                "{\"rules\":[]}".getBytes(StandardCharsets.UTF_8)));

        Long assignmentId = assignmentService.create(createForm);

        AssignmentForm updateForm = new AssignmentForm();
        updateForm.setAssignmentName("Persistent Lab Updated");
        updateForm.setSemesterId(semesterId);
        updateForm.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        updateForm.setPlagiarismThreshold(30);
        updateForm.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        updateForm.setLogicWeight(70);
        updateForm.setOopWeight(30);

        assignmentService.update(assignmentId, updateForm);

        AssignmentAttachment descriptionAttachment = assignmentAttachmentRepository
                .findByAssignmentIdAndAttachmentType(assignmentId, AssignmentAttachmentType.DESCRIPTION)
                .orElseThrow();
        AssignmentAttachment oopRuleAttachment = assignmentAttachmentRepository
                .findByAssignmentIdAndAttachmentType(assignmentId, AssignmentAttachmentType.OOP_RULE_CONFIG)
                .orElseThrow();

        assertThat(descriptionAttachment.getFileName()).isEqualTo("brief.md");
        assertThat(descriptionAttachment.getData()).isEqualTo("# Persistent Lab".getBytes(StandardCharsets.UTF_8));
        assertThat(oopRuleAttachment.getFileName()).isEqualTo("rules.json");
        assertThat(oopRuleAttachment.getData()).isEqualTo("{\"rules\":[]}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void oopRuleCheckerFactoryReadsRuleConfigFromAttachmentStorage() {
        Long semesterId = createSemester("AT28");

        byte[] oopRuleBytes = """
                {"rules":[{"type":"minimum_class_count","value":3,"label":"At least three classes"}]}
                """.trim().getBytes(StandardCharsets.UTF_8);

        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Rules Lab");
        form.setSemesterId(semesterId);
        form.setGradingMode(com.group4.javagrader.entity.GradingMode.OOP);
        form.setPlagiarismThreshold(25);
        form.setOutputNormalizationPolicy(com.group4.javagrader.entity.OutputNormalizationPolicy.STRICT);
        form.setLogicWeight(60);
        form.setOopWeight(40);
        form.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                oopRuleBytes));

        Long assignmentId = assignmentService.create(form);
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();

        List<OopRuleChecker> checkers = oopRuleCheckerFactory.create(assignment);
        assertThat(checkers).hasSize(1);
        assertThat(checkers.get(0).check(List.of(
                "public class Demo { }"
        )).ruleLabel()).isEqualTo("At least three classes");
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
}
