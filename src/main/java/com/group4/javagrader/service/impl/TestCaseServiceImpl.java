package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.dto.TestCaseImportPreviewForm;
import com.group4.javagrader.dto.TestCaseImportRowForm;
import com.group4.javagrader.entity.Problem;
import com.group4.javagrader.entity.TestCase;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.ProblemRepository;
import com.group4.javagrader.repository.TestCaseRepository;
import com.group4.javagrader.repository.TestCaseResultRepository;
import com.group4.javagrader.service.TestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private static final Pattern ZIP_WEIGHT_SUFFIX = Pattern.compile("^(.*)__w([0-9]+(?:\\.[0-9]+)?)$");
    private static final Set<String> EXPECTED_OUTPUT_HEADERS = Set.of("expectedoutput", "expected", "output");
    private static final Set<String> INPUT_HEADERS = Set.of("inputdata", "input", "stdin");
    private static final Set<String> WEIGHT_HEADERS = Set.of("weight", "score", "points");
    private static final Set<String> SOURCE_HEADERS = Set.of("sourcename", "source", "name", "caseid", "case");

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseResultRepository testCaseResultRepository;

    public TestCaseServiceImpl(
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository,
            TestCaseResultRepository testCaseResultRepository) {
        this.testCaseRepository = testCaseRepository;
        this.problemRepository = problemRepository;
        this.testCaseResultRepository = testCaseResultRepository;
    }

    @Override
    @Transactional
    public Long create(TestCaseForm form) {
        Problem problem = loadProblem(form.getProblemId());

        TestCase testCase = buildTestCase(
                problem,
                nextCaseOrder(form.getProblemId()),
                form.getInputData(),
                form.getExpectedOutput(),
                form.getWeight());

        return testCaseRepository.save(testCase).getId();
    }

    @Override
    @Transactional
    public void update(Long id, TestCaseForm form) {
        TestCase testCase = loadTestCase(id);
        ensureTestCaseBelongsToProblem(testCase, form.getProblemId());

        TestCase updatedTestCase = buildTestCase(
                testCase.getProblem(),
                testCase.getCaseOrder(),
                form.getInputData(),
                form.getExpectedOutput(),
                form.getWeight());
        updatedTestCase.setId(testCase.getId());
        updatedTestCase.setSample(testCase.isSample());
        testCaseRepository.save(updatedTestCase);
    }

    @Override
    @Transactional
    public void delete(Long problemId, Long testCaseId) {
        TestCase testCase = loadTestCase(testCaseId);
        ensureTestCaseBelongsToProblem(testCase, problemId);
        if (testCaseResultRepository.existsByTestCaseId(testCaseId)) {
            throw new WorkflowStateException("Cannot delete testcase after grading results exist.");
        }
        testCaseRepository.delete(testCase);
    }

    @Override
    @Transactional(readOnly = true)
    public TestCaseImportPreviewForm buildImportPreview(Long problemId, MultipartFile file) {
        loadProblem(problemId);

        String fileName = normalizeImportFileName(file);
        String extension = resolveExtension(fileName);

        List<TestCaseImportRowForm> rows = switch (extension) {
            case "zip" -> parseZipImport(file);
            case "csv" -> parseCsvImport(file);
            default -> throw new InputValidationException("Import file must be a .zip or .csv file.");
        };

        if (rows.isEmpty()) {
            throw new InputValidationException("Import file did not contain any testcase rows.");
        }

        TestCaseImportPreviewForm previewForm = new TestCaseImportPreviewForm();
        previewForm.setProblemId(problemId);
        previewForm.setRows(rows);
        return previewForm;
    }

    @Override
    @Transactional
    public long saveImportedTestCases(TestCaseImportPreviewForm previewForm) {
        Problem problem = loadProblem(previewForm.getProblemId());
        List<TestCaseImportRowForm> rows = previewForm.getRows();

        if (rows == null || rows.isEmpty()) {
            throw new InputValidationException("There is no imported testcase to save.");
        }

        int caseOrder = nextCaseOrder(problem.getId());
        List<TestCase> entities = new ArrayList<>();
        Set<String> seenSourceNames = new LinkedHashSet<>();

        for (TestCaseImportRowForm row : rows) {
            validateImportedRow(row, seenSourceNames);
            entities.add(buildTestCase(
                    problem,
                    caseOrder++,
                    row.getInputData(),
                    row.getExpectedOutput(),
                    row.getWeight()));
        }

        testCaseRepository.saveAll(entities);
        return entities.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCase> findByProblemId(Long problemId) {
        return testCaseRepository.findByProblemIdOrderByCaseOrderAsc(problemId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByProblemId(Long problemId) {
        return testCaseRepository.countByProblemId(problemId);
    }

    private Problem loadProblem(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found."));
    }

    private int nextCaseOrder(Long problemId) {
        return testCaseRepository.findMaxCaseOrderByProblemId(problemId) + 1;
    }

    private TestCase buildTestCase(
            Problem problem,
            int caseOrder,
            String inputData,
            String expectedOutput,
            BigDecimal weight) {
        if (!StringUtils.hasText(expectedOutput)) {
            throw new InputValidationException("Expected output is required.");
        }
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InputValidationException("Weight must be greater than 0.");
        }

        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setCaseOrder(caseOrder);
        testCase.setInputData(emptyToNull(inputData));
        testCase.setExpectedOutput(normalizeText(expectedOutput));
        testCase.setWeight(weight);
        testCase.setSample(false);
        return testCase;
    }

    private List<TestCaseImportRowForm> parseZipImport(MultipartFile file) {
        Map<String, ZipDraft> drafts = new TreeMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(file.getBytes()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryFileName = extractFileName(entry.getName());
                if (!StringUtils.hasText(entryFileName) || entryFileName.startsWith(".")) {
                    continue;
                }

                String lowerCaseName = entryFileName.toLowerCase(Locale.ROOT);
                if (!lowerCaseName.endsWith(".in") && !lowerCaseName.endsWith(".out")) {
                    continue;
                }

                ParsedZipName parsedZipName = parseZipName(entryFileName.substring(0, entryFileName.lastIndexOf('.')));
                ZipDraft draft = drafts.computeIfAbsent(
                        parsedZipName.sourceName(),
                        key -> new ZipDraft(parsedZipName.sourceName(), parsedZipName.weight()));

                if (draft.weight().compareTo(parsedZipName.weight()) != 0) {
                    throw new InputValidationException("ZIP testcase weight is inconsistent for '" + parsedZipName.sourceName() + "'.");
                }

                String content = normalizeText(new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                if (lowerCaseName.endsWith(".in")) {
                    if (draft.inputData() != null) {
                        throw new InputValidationException("ZIP import contains duplicate .in file for '" + parsedZipName.sourceName() + "'.");
                    }
                    draft.setInputData(content);
                } else {
                    if (draft.expectedOutput() != null) {
                        throw new InputValidationException("ZIP import contains duplicate .out file for '" + parsedZipName.sourceName() + "'.");
                    }
                    draft.setExpectedOutput(content);
                }
            }
        } catch (IOException ex) {
            throw new InputValidationException("ZIP testcase import could not be read.", ex);
        }

        if (drafts.isEmpty()) {
            throw new InputValidationException("ZIP import must contain .in/.out testcase files.");
        }

        List<TestCaseImportRowForm> rows = new ArrayList<>();
        for (ZipDraft draft : drafts.values()) {
            if (!StringUtils.hasText(draft.expectedOutput())) {
                throw new InputValidationException("ZIP import is missing the .out file for testcase '" + draft.sourceName() + "'.");
            }

            TestCaseImportRowForm row = new TestCaseImportRowForm();
            row.setSourceName(draft.sourceName());
            row.setInputData(defaultString(draft.inputData()));
            row.setExpectedOutput(draft.expectedOutput());
            row.setWeight(draft.weight());
            rows.add(row);
        }

        return rows;
    }

    private List<TestCaseImportRowForm> parseCsvImport(MultipartFile file) {
        String csvContent;
        try {
            csvContent = normalizeText(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new InputValidationException("CSV testcase import could not be read.", ex);
        }

        if (csvContent.startsWith("\uFEFF")) {
            csvContent = csvContent.substring(1);
        }

        List<List<String>> records = parseCsvRecords(csvContent);
        if (records.size() < 2) {
            throw new InputValidationException("CSV import must include a header row and at least one testcase row.");
        }

        List<String> header = records.get(0);
        int expectedOutputIndex = findHeaderIndex(header, EXPECTED_OUTPUT_HEADERS);
        if (expectedOutputIndex < 0) {
            throw new InputValidationException("CSV import must include an expectedOutput column.");
        }

        int inputIndex = findHeaderIndex(header, INPUT_HEADERS);
        int weightIndex = findHeaderIndex(header, WEIGHT_HEADERS);
        int sourceIndex = findHeaderIndex(header, SOURCE_HEADERS);

        List<TestCaseImportRowForm> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.stream().allMatch(value -> !StringUtils.hasText(value))) {
                continue;
            }

            String expectedOutput = valueAt(record, expectedOutputIndex);
            if (!StringUtils.hasText(expectedOutput)) {
                throw new InputValidationException("CSV row " + (i + 1) + " is missing expected output.");
            }

            TestCaseImportRowForm row = new TestCaseImportRowForm();
            row.setSourceName(resolveSourceName(sourceIndex >= 0 ? valueAt(record, sourceIndex) : null, i));
            row.setInputData(defaultString(inputIndex >= 0 ? valueAt(record, inputIndex) : null));
            row.setExpectedOutput(expectedOutput);
            row.setWeight(parseWeight(weightIndex >= 0 ? valueAt(record, weightIndex) : null, "CSV row " + (i + 1) + " has an invalid weight."));
            rows.add(row);
        }

        if (rows.isEmpty()) {
            throw new InputValidationException("CSV import did not contain any testcase rows.");
        }

        return rows;
    }

    private List<List<String>> parseCsvRecords(String csvContent) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < csvContent.length(); i++) {
            char currentChar = csvContent.charAt(i);
            if (insideQuotes) {
                if (currentChar == '"') {
                    if (i + 1 < csvContent.length() && csvContent.charAt(i + 1) == '"') {
                        currentCell.append('"');
                        i++;
                    } else {
                        insideQuotes = false;
                    }
                } else {
                    currentCell.append(currentChar);
                }
                continue;
            }

            switch (currentChar) {
                case '"' -> insideQuotes = true;
                case ',' -> {
                    currentRow.add(currentCell.toString());
                    currentCell.setLength(0);
                }
                case '\n' -> {
                    currentRow.add(currentCell.toString());
                    records.add(currentRow);
                    currentRow = new ArrayList<>();
                    currentCell.setLength(0);
                }
                default -> currentCell.append(currentChar);
            }
        }

        if (insideQuotes) {
            throw new InputValidationException("CSV import has an unterminated quoted value.");
        }

        currentRow.add(currentCell.toString());
        records.add(currentRow);
        return records;
    }

    private ParsedZipName parseZipName(String rawBaseName) {
        Matcher matcher = ZIP_WEIGHT_SUFFIX.matcher(rawBaseName);
        if (!matcher.matches()) {
            return new ParsedZipName(rawBaseName, BigDecimal.ONE);
        }

        String sourceName = matcher.group(1);
        if (!StringUtils.hasText(sourceName)) {
            throw new InputValidationException("ZIP testcase name is invalid: '" + rawBaseName + "'.");
        }

        return new ParsedZipName(sourceName, parseWeight(matcher.group(2), "ZIP testcase weight is invalid for '" + rawBaseName + "'."));
    }

    private void validateImportedRow(TestCaseImportRowForm row, Set<String> seenSourceNames) {
        if (!StringUtils.hasText(row.getSourceName())) {
            throw new InputValidationException("Each imported testcase must have a source name.");
        }
        if (!StringUtils.hasText(row.getExpectedOutput())) {
            throw new InputValidationException("Each imported testcase must have expected output.");
        }
        if (row.getWeight() == null || row.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InputValidationException("Each imported testcase must have a weight greater than 0.");
        }

        String normalizedSourceName = row.getSourceName().trim();
        row.setSourceName(normalizedSourceName);
        if (!seenSourceNames.add(normalizedSourceName.toLowerCase(Locale.ROOT))) {
            throw new InputValidationException("Imported testcase names must be unique inside the preview.");
        }
    }

    private String normalizeImportFileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new InputValidationException("Please choose a ZIP or CSV file to import.");
        }
        if (file.isEmpty()) {
            throw new InputValidationException("Import file must not be empty.");
        }

        String fileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(fileName) || fileName.contains("..")) {
            throw new InputValidationException("Import file name is invalid.");
        }
        return fileName;
    }

    private String resolveExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new InputValidationException("Import file must be a .zip or .csv file.");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private int findHeaderIndex(List<String> header, Set<String> acceptedNames) {
        for (int i = 0; i < header.size(); i++) {
            String normalized = header.get(i) == null
                    ? ""
                    : header.get(i).replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
            if (acceptedNames.contains(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private BigDecimal parseWeight(String rawWeight, String errorMessage) {
        if (!StringUtils.hasText(rawWeight)) {
            return BigDecimal.ONE;
        }
        try {
            BigDecimal weight = new BigDecimal(rawWeight.trim());
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InputValidationException(errorMessage);
            }
            return weight;
        } catch (NumberFormatException ex) {
            throw new InputValidationException(errorMessage, ex);
        }
    }

    private String resolveSourceName(String rawSourceName, int rowIndex) {
        if (!StringUtils.hasText(rawSourceName)) {
            return "row-" + rowIndex;
        }
        return rawSourceName.trim();
    }

    private String valueAt(List<String> record, int index) {
        if (index < 0 || index >= record.size()) {
            return null;
        }
        return record.get(index);
    }

    private String extractFileName(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String emptyToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return normalizeText(value);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private TestCase loadTestCase(Long testCaseId) {
        return testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Testcase not found."));
    }

    private void ensureTestCaseBelongsToProblem(TestCase testCase, Long problemId) {
        if (!testCase.getProblem().getId().equals(problemId)) {
            throw new OwnershipViolationException("Testcase does not belong to this problem.");
        }
    }

    private record ParsedZipName(String sourceName, BigDecimal weight) {
    }

    private static final class ZipDraft {
        private final String sourceName;
        private final BigDecimal weight;
        private String inputData;
        private String expectedOutput;

        private ZipDraft(String sourceName, BigDecimal weight) {
            this.sourceName = sourceName;
            this.weight = weight;
        }

        private String sourceName() {
            return sourceName;
        }

        private BigDecimal weight() {
            return weight;
        }

        private String inputData() {
            return inputData;
        }

        private void setInputData(String inputData) {
            this.inputData = inputData;
        }

        private String expectedOutput() {
            return expectedOutput;
        }

        private void setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }
}
