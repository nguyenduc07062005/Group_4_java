package com.group4.javagrader.service;

import com.group4.javagrader.dto.ResultIndexView;

import java.util.Optional;

public interface ReportService {

    Optional<ResultIndexView> buildReportIndex(Long assignmentId);

    byte[] exportGradebookCsv(Long assignmentId);

    byte[] exportDetailCsv(Long assignmentId);

    byte[] exportSummaryPdf(Long assignmentId);
}
