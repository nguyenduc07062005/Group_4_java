package com.group4.javagrader.service;

import com.group4.javagrader.dto.PlagiarismDashboardView;
import com.group4.javagrader.entity.PlagiarismPair;
import com.group4.javagrader.entity.PlagiarismReport;

import java.util.List;
import java.util.Optional;

public interface PlagiarismService {

    PlagiarismReport runReport(Long assignmentId, String username);

    Optional<PlagiarismReport> findLatestReport(Long assignmentId);

    List<PlagiarismPair> findPairsByReportId(Long reportId);

    PlagiarismDashboardView buildDashboard(Long assignmentId);

    PlagiarismPair overridePair(Long assignmentId, Long pairId, String decision, String note, String username);
}
