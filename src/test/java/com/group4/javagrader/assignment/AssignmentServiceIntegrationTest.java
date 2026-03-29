package com.group4.javagrader.assignment;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AssignmentServiceIntegrationTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void createStoresPhaseTwoAssignmentConfiguration() {
        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Collections Lab");
        form.setGradingMode("OOP");
        form.setPlagiarismThreshold(35);
        form.setOutputNormalizationPolicy("TRIM_ALL");
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

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(assignment.getAssignmentName()).isEqualTo("Collections Lab");
        assertThat(assignment.getGradingMode()).isEqualTo("OOP");
        assertThat(assignment.getPlagiarismThreshold()).isEqualByComparingTo("35.00");
        assertThat(assignment.getOutputNormalizationPolicy()).isEqualTo("TRIM_ALL");
        assertThat(assignment.getLogicWeight()).isEqualTo(60);
        assertThat(assignment.getOopWeight()).isEqualTo(40);
        assertThat(assignment.getDescriptionFileName()).isEqualTo("brief.md");
        assertThat(assignment.getOopRuleConfigFileName()).isEqualTo("rules.json");
        assertThat(assignment.getSemester().getCode()).startsWith("AUTO-");
        assertThat(assignment.getDescription()).contains("Collections Lab");
    }

    @Test
    void createRejectsUnsupportedDescriptionUpload() {
        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Broken Upload");
        form.setGradingMode("JAVA_CORE");
        form.setPlagiarismThreshold(20);
        form.setOutputNormalizationPolicy("STRICT");
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
        AssignmentForm form = new AssignmentForm();
        form.setAssignmentName("Core Lab");
        form.setGradingMode("JAVA_CORE");
        form.setPlagiarismThreshold(25);
        form.setOutputNormalizationPolicy("STRICT");
        form.setLogicWeight(35);
        form.setOopWeight(65);
        form.setOopRuleConfig(new MockMultipartFile(
                "oopRuleConfig",
                "rules.json",
                "application/json",
                "{\"rules\":[\"unused\"]}".getBytes(StandardCharsets.UTF_8)));

        Long assignmentId = assignmentService.create(form);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(assignment.getGradingMode()).isEqualTo("JAVA_CORE");
        assertThat(assignment.getLogicWeight()).isEqualTo(100);
        assertThat(assignment.getOopWeight()).isEqualTo(0);
        assertThat(assignment.getOopRuleConfigFileName()).isNull();
        assertThat(assignment.getOopRuleConfigContentType()).isNull();
        assertThat(assignment.getOopRuleConfigData()).isNull();
    }
}
