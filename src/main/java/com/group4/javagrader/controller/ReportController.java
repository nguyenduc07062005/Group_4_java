package com.group4.javagrader.controller;

import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Controller
@RequestMapping("/assignments/{assignmentId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String showIndex(@PathVariable("assignmentId") Long assignmentId) {
        return "redirect:/assignments/" + assignmentId + "/grading#export-reports";
    }

    @GetMapping("/gradebook.csv")
    public ResponseEntity<byte[]> downloadGradebookCsv(@PathVariable("assignmentId") Long assignmentId) {
        return downloadReport(
                () -> reportService.exportGradebookCsv(assignmentId),
                "gradebook.csv",
                "text/csv");
    }

    @GetMapping("/detail.csv")
    public ResponseEntity<byte[]> downloadDetailCsv(@PathVariable("assignmentId") Long assignmentId) {
        return downloadReport(
                () -> reportService.exportDetailCsv(assignmentId),
                "detail.csv",
                "text/csv");
    }

    @GetMapping("/summary.pdf")
    public ResponseEntity<byte[]> downloadSummaryPdf(@PathVariable("assignmentId") Long assignmentId) {
        return downloadReport(
                () -> reportService.exportSummaryPdf(assignmentId),
                "summary.pdf",
                MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> downloadReport(
            Supplier<byte[]> exporter,
            String fileName,
            String contentType) {
        try {
            return buildAttachment(fileName, contentType, exporter.get());
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (WorkflowStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(ex.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    private ResponseEntity<byte[]> buildAttachment(String fileName, String contentType, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
