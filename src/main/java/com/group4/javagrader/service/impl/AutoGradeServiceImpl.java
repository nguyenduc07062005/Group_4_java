package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.AutoGradeResult;
import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.service.AutoGradeService;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.PlagiarismService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AutoGradeServiceImpl implements AutoGradeService {

    private static final Logger log = LoggerFactory.getLogger(AutoGradeServiceImpl.class);

    private final PlagiarismService plagiarismService;
    private final BatchService batchService;

    public AutoGradeServiceImpl(PlagiarismService plagiarismService, BatchService batchService) {
        this.plagiarismService = plagiarismService;
        this.batchService = batchService;
    }

    @Override
    public AutoGradeResult run(Long assignmentId, String username) {
        PlagiarismReport report;
        try {
            log.info("Auto-grade [assignment={}]: running plagiarism check", assignmentId);
            report = plagiarismService.runReport(assignmentId, username);
        } catch (Exception ex) {
            log.error("Auto-grade [assignment={}]: plagiarism check failed", assignmentId, ex);
            return AutoGradeResult.failure("Plagiarism Check", ex.getMessage(), null, null);
        }

        Batch batch;
        try {
            log.info("Auto-grade [assignment={}]: creating batch snapshot", assignmentId);
            batch = batchService.createBatch(assignmentId, username);
        } catch (Exception ex) {
            log.error("Auto-grade [assignment={}]: batch creation failed", assignmentId, ex);
            return AutoGradeResult.failure("Create Snapshot", ex.getMessage(), report, null);
        }

        try {
            log.info("Auto-grade [assignment={}]: starting batch {}", assignmentId, batch.getId());
            batch = batchService.startBatch(assignmentId, batch.getId(), username);
        } catch (Exception ex) {
            log.error("Auto-grade [assignment={}]: batch start failed", assignmentId, ex);
            return AutoGradeResult.failure("Start Grading", ex.getMessage(), report, batch);
        }

        log.info("Auto-grade [assignment={}]: pipeline started successfully, batch={}", assignmentId, batch.getId());
        return AutoGradeResult.success(report, batch);
    }
}
