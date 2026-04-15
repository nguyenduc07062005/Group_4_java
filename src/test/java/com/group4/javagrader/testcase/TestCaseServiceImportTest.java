package com.group4.javagrader.testcase;

import com.group4.javagrader.dto.TestCaseImportPreviewForm;
import com.group4.javagrader.dto.TestCaseImportRowForm;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.impl.TestCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestCaseServiceImportTest {

    private final TestCaseRepository testCaseRepository = mock(TestCaseRepository.class);
    private final ProblemRepository problemRepository = mock(ProblemRepository.class);
    private final TestCaseResultRepository testCaseResultRepository = mock(TestCaseResultRepository.class);
    private final TestCaseServiceImpl service = new TestCaseServiceImpl(testCaseRepository, problemRepository, testCaseResultRepository);

    @Test
    void buildsPreviewFromZipImport() throws Exception {
        when(problemRepository.findById(42L)).thenReturn(Optional.of(problem(42L)));

        MockMultipartFile file = new MockMultipartFile(
                "importFile",
                "cases.zip",
                "application/zip",
                zipOf(
                        "sum01__w2.in", "1 2",
                        "sum01__w2.out", "3",
                        "sum02.out", "10"
                ));

        TestCaseImportPreviewForm preview = service.buildImportPreview(42L, file);

        assertThat(preview.getProblemId()).isEqualTo(42L);
        assertThat(preview.getRows()).hasSize(2);
        assertThat(preview.getRows().get(0).getSourceName()).isEqualTo("sum01");
        assertThat(preview.getRows().get(0).getInputData()).isEqualTo("1 2");
        assertThat(preview.getRows().get(0).getExpectedOutput()).isEqualTo("3");
        assertThat(preview.getRows().get(0).getWeight()).isEqualByComparingTo("2");
        assertThat(preview.getRows().get(1).getSourceName()).isEqualTo("sum02");
        assertThat(preview.getRows().get(1).getWeight()).isEqualByComparingTo("1");
    }

    @Test
    void buildsPreviewFromCsvImport() {
        when(problemRepository.findById(42L)).thenReturn(Optional.of(problem(42L)));

        MockMultipartFile file = new MockMultipartFile(
                "importFile",
                "cases.csv",
                "text/csv",
                ("sourceName,inputData,expectedOutput,weight,sample\n"
                        + "sum-01,1 2,3,2.5,true\n"
                        + "sum-02,,10,,false\n").getBytes(StandardCharsets.UTF_8));

        TestCaseImportPreviewForm preview = service.buildImportPreview(42L, file);

        assertThat(preview.getRows()).hasSize(2);
        assertThat(preview.getRows().get(0).getSourceName()).isEqualTo("sum-01");
        assertThat(preview.getRows().get(0).getWeight()).isEqualByComparingTo("2.5");
        assertThat(preview.getRows().get(1).getSourceName()).isEqualTo("sum-02");
        assertThat(preview.getRows().get(1).getWeight()).isEqualByComparingTo("1");
    }

    @Test
    void buildImportPreviewRejectsUnsupportedFileTypeWithTypedValidationException() {
        when(problemRepository.findById(42L)).thenReturn(Optional.of(problem(42L)));

        MockMultipartFile file = new MockMultipartFile(
                "importFile",
                "cases.txt",
                "text/plain",
                "not an import".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.buildImportPreview(42L, file))
                .isInstanceOf(InputValidationException.class)
                .hasMessage("Import file must be a .zip or .csv file.");
    }

    @Test
    void updateRejectsTestcaseThatBelongsToAnotherProblemWithTypedOwnershipException() {
        TestCase existing = testCase(7L, problem(99L));
        when(testCaseRepository.findById(7L)).thenReturn(Optional.of(existing));

        TestCaseForm form = new TestCaseForm();
        form.setProblemId(42L);
        form.setExpectedOutput("3");
        form.setWeight(BigDecimal.ONE);

        assertThatThrownBy(() -> service.update(7L, form))
                .isInstanceOf(OwnershipViolationException.class)
                .hasMessage("Testcase does not belong to this problem.");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void savesImportedTestCasesWithSequentialCaseOrder() {
        when(problemRepository.findById(42L)).thenReturn(Optional.of(problem(42L)));
        when(testCaseRepository.findMaxCaseOrderByProblemId(42L)).thenReturn(2);
        when(testCaseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TestCaseImportRowForm firstRow = new TestCaseImportRowForm();
        firstRow.setSourceName("sum01");
        firstRow.setInputData("1 2");
        firstRow.setExpectedOutput("3");
        firstRow.setWeight(BigDecimal.valueOf(2));

        TestCaseImportRowForm secondRow = new TestCaseImportRowForm();
        secondRow.setSourceName("sum02");
        secondRow.setExpectedOutput("10");
        secondRow.setWeight(BigDecimal.ONE);

        TestCaseImportPreviewForm previewForm = new TestCaseImportPreviewForm();
        previewForm.setProblemId(42L);
        previewForm.setRows(List.of(firstRow, secondRow));

        long savedCount = service.saveImportedTestCases(previewForm);

        assertThat(savedCount).isEqualTo(2);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(testCaseRepository).saveAll(captor.capture());
        List<TestCase> savedCases = captor.getValue();

        assertThat(savedCases).hasSize(2);
        assertThat(savedCases.get(0).getCaseOrder()).isEqualTo(3);
        assertThat(savedCases.get(0).getWeight()).isEqualByComparingTo("2");
        assertThat(savedCases.get(1).getCaseOrder()).isEqualTo(4);
        assertThat(savedCases.get(1).isSample()).isFalse();
    }

    private Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setId(id);
        return problem;
    }

    private TestCase testCase(Long id, Problem problem) {
        TestCase testCase = new TestCase();
        testCase.setId(id);
        testCase.setProblem(problem);
        return testCase;
    }

    private byte[] zipOf(String... nameContentPairs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                zipOutputStream.putNextEntry(new ZipEntry(nameContentPairs[i]));
                zipOutputStream.write(nameContentPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}
