package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.ResultDetailView;
import com.group4.javagrader.dto.ResultIndexView;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.service.ReportService;
import com.group4.javagrader.service.ResultService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final float PDF_MARGIN = 48f;
    private static final float PDF_FONT_SIZE = 11f;
    private static final float PDF_LINE_HEIGHT = 16f;
    private static final String DANGEROUS_CSV_PREFIXES = "=+-@";
    private static final List<String> PDF_FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/Library/Fonts/Arial Unicode.ttf");

    private final ResultService resultService;

    public ReportServiceImpl(ResultService resultService) {
        this.resultService = resultService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultIndexView> buildReportIndex(Long assignmentId) {
        return resultService.buildIndex(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGradebookCsv(Long assignmentId) {
        ResultIndexView indexView = requireLatestTerminalIndex(assignmentId);

        List<String> lines = new ArrayList<>();
        lines.add("Submitter,Status,Score,Max Score,Passed Testcases,Total Testcases");
        indexView.getResults().forEach(result -> lines.add(csvLine(
                result.getSubmitterName(),
                result.getStatus(),
                result.getTotalScore().toPlainString(),
                result.getMaxScore().toPlainString(),
                String.valueOf(result.getTestcasePassedCount()),
                String.valueOf(result.getTestcaseTotalCount()))));
        return csvBytes(lines);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDetailCsv(Long assignmentId) {
        requireLatestTerminalIndex(assignmentId);
        List<ResultDetailView> detailViews = resultService.buildDetailsForLatestBatch(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        List<String> lines = new ArrayList<>();
        lines.add("Submitter,Problem,Test Case,Status,Earned Score,Expected Output,Actual Output,Message");
        for (ResultDetailView detailView : detailViews) {
            detailView.getProblemResults().forEach(problem ->
                    problem.getTestCases().forEach(testCase -> lines.add(csvLine(
                            detailView.getSubmission().getSubmitterName(),
                            problem.getTitle(),
                            String.valueOf(testCase.getCaseOrder()),
                            testCase.getStatus(),
                            testCase.getEarnedScore().toPlainString(),
                            testCase.getExpectedOutput(),
                            testCase.getActualOutput(),
                            testCase.getMessage()))));
        }
        return csvBytes(lines);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSummaryPdf(Long assignmentId) {
        ResultIndexView indexView = requireLatestTerminalIndex(assignmentId);

        List<String> lines = new ArrayList<>();
        Batch latestBatch = indexView.getLatestBatch();
        lines.add("Java Code Grader Summary Report");
        lines.add("");
        lines.add("Assignment: " + indexView.getAssignment().getAssignmentName());
        lines.add("Semester: " + indexView.getAssignment().getSemester().getCode() + " - " + indexView.getAssignment().getSemester().getName());
        lines.add("Mode: " + indexView.getAssignment().getGradingMode());
        lines.add("Latest batch: " + (latestBatch != null ? latestBatch.getStatus() : "No batch yet"));
        lines.add("Stored results: " + indexView.getResults().size());
        lines.add("");
        lines.add("Submitter - Status - Score");
        indexView.getResults().forEach(result -> lines.add(
                result.getSubmitterName() + " - " + result.getStatus() + " - " + result.getTotalScore() + "/" + result.getMaxScore()));
        return buildReadablePdf(lines);
    }

    private ResultIndexView requireLatestTerminalIndex(Long assignmentId) {
        ResultIndexView indexView = resultService.buildIndex(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        if (!isTerminal(indexView.getLatestBatch())) {
            throw new WorkflowStateException("Reports are only available after the latest batch reaches a terminal state.");
        }
        return indexView;
    }

    private boolean isTerminal(Batch batch) {
        return batch != null
                && (batch.getStatus() == BatchStatus.COMPLETED
                || batch.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS);
    }

    private String csvLine(String... values) {
        return java.util.Arrays.stream(values)
                .map(this::escapeCsv)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String escapeCsv(String value) {
        String safeValue = neutralizeCsvFormula(value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n'));
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private String neutralizeCsvFormula(String value) {
        String leadingTrimmed = value.stripLeading();
        if (!leadingTrimmed.isEmpty() && DANGEROUS_CSV_PREFIXES.indexOf(leadingTrimmed.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private byte[] csvBytes(List<String> lines) {
        return ("\uFEFF" + String.join("\r\n", lines)).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildReadablePdf(List<String> lines) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ResolvedPdfFont resolvedFont = resolvePdfFont(document);
            writePdfLines(document, resolvedFont, lines);
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("PDF summary could not be generated.", ex);
        }
    }

    private void writePdfLines(PDDocument document, ResolvedPdfFont resolvedFont, List<String> lines) throws IOException {
        PDRectangle pageSize = PDRectangle.A4;
        float usableWidth = pageSize.getWidth() - (PDF_MARGIN * 2);
        float startY = pageSize.getHeight() - PDF_MARGIN;

        PDPage page = new PDPage(pageSize);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        try {
            contentStream.beginText();
            contentStream.setFont(resolvedFont.font(), PDF_FONT_SIZE);
            contentStream.setLeading(PDF_LINE_HEIGHT);
            contentStream.newLineAtOffset(PDF_MARGIN, startY);

            float currentY = startY;
            for (String line : lines) {
                List<String> wrappedLines = wrapLine(line, resolvedFont, usableWidth);
                for (String wrappedLine : wrappedLines) {
                    if (currentY <= PDF_MARGIN) {
                        contentStream.endText();
                        contentStream.close();

                        page = new PDPage(pageSize);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.beginText();
                        contentStream.setFont(resolvedFont.font(), PDF_FONT_SIZE);
                        contentStream.setLeading(PDF_LINE_HEIGHT);
                        contentStream.newLineAtOffset(PDF_MARGIN, startY);
                        currentY = startY;
                    }

                    if (!wrappedLine.isEmpty()) {
                        contentStream.showText(wrappedLine);
                    }
                    contentStream.newLine();
                    currentY -= PDF_LINE_HEIGHT;
                }
            }

            contentStream.endText();
        } finally {
            contentStream.close();
        }
    }

    private List<String> wrapLine(String rawLine, ResolvedPdfFont resolvedFont, float maxWidth) throws IOException {
        String line = normalizePdfText(rawLine, resolvedFont.unicodeCapable());
        if (line.isBlank()) {
            return List.of("");
        }

        List<String> wrapped = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textWidth(candidate, resolvedFont.font()) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (!current.isEmpty()) {
                wrapped.add(current.toString());
                current.setLength(0);
            }

            if (textWidth(word, resolvedFont.font()) <= maxWidth) {
                current.append(word);
            } else {
                wrapped.addAll(splitLongWord(word, resolvedFont.font(), maxWidth));
            }
        }

        if (!current.isEmpty()) {
            wrapped.add(current.toString());
        }
        return wrapped;
    }

    private List<String> splitLongWord(String word, PDFont font, float maxWidth) throws IOException {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char character : word.toCharArray()) {
            String candidate = current.toString() + character;
            if (textWidth(candidate, font) <= maxWidth || current.isEmpty()) {
                current.append(character);
                continue;
            }
            parts.add(current.toString());
            current.setLength(0);
            current.append(character);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }

    private float textWidth(String text, PDFont font) throws IOException {
        return font.getStringWidth(text) / 1000f * PDF_FONT_SIZE;
    }

    private String normalizePdfText(String value, boolean unicodeCapable) {
        String safeValue = value == null ? "" : value.replace('\t', ' ');
        return unicodeCapable ? safeValue : safeValue.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private ResolvedPdfFont resolvePdfFont(PDDocument document) throws IOException {
        for (String candidate : PDF_FONT_CANDIDATES) {
            Path path = Path.of(candidate);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try (InputStream inputStream = Files.newInputStream(path)) {
                return new ResolvedPdfFont(PDType0Font.load(document, inputStream), true);
            } catch (IOException ignored) {
                // Fall through to the next candidate font.
            }
        }
        return new ResolvedPdfFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), false);
    }

    private record ResolvedPdfFont(PDFont font, boolean unicodeCapable) {
    }
}
