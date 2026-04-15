package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.BatchProgressView;
import com.group4.javagrader.dto.BatchPrecheckView;
import com.group4.javagrader.dto.GradingWorkspaceView;
import com.group4.javagrader.dto.ResultDetailView;
import com.group4.javagrader.dto.ResultIndexView;
import com.group4.javagrader.exception.DomainException;
import com.group4.javagrader.service.BatchService;
import com.group4.javagrader.service.GradingWorkspaceService;
import com.group4.javagrader.service.ResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GradingWorkspaceServiceImpl implements GradingWorkspaceService {

    private final BatchService batchService;
    private final ResultService resultService;

    public GradingWorkspaceServiceImpl(BatchService batchService, ResultService resultService) {
        this.batchService = batchService;
        this.resultService = resultService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GradingWorkspaceView> build(Long assignmentId, Long selectedResultId) {
        BatchPrecheckView precheck;
        try {
            precheck = batchService.buildPrecheck(assignmentId);
        } catch (DomainException ex) {
            return Optional.empty();
        }

        Optional<ResultIndexView> resultIndexOptional = resultService.buildIndex(assignmentId);
        if (resultIndexOptional.isEmpty()) {
            return Optional.empty();
        }
        ResultIndexView resultIndex = resultIndexOptional.get();

        BatchProgressView latestProgress = precheck.getLatestBatch() == null
                ? null
                : batchService.buildProgress(assignmentId, precheck.getLatestBatch().getId()).orElse(null);

        ResultDetailView selectedResultDetail = null;
        Long effectiveSelectedResultId = selectedResultId;
        if (effectiveSelectedResultId == null && !resultIndex.getResults().isEmpty()) {
            effectiveSelectedResultId = resultIndex.getResults().get(0).getResultId();
        }
        if (effectiveSelectedResultId != null) {
            selectedResultDetail = resultService.buildDetail(assignmentId, effectiveSelectedResultId).orElse(null);
            if (selectedResultDetail == null) {
                effectiveSelectedResultId = null;
            }
        }

        return Optional.of(new GradingWorkspaceView(
                precheck,
                latestProgress,
                resultIndex,
                selectedResultDetail,
                effectiveSelectedResultId));
    }
}
