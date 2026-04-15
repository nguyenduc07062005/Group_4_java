package com.group4.javagrader.service;

import com.group4.javagrader.dto.BatchPrecheckView;
import com.group4.javagrader.dto.BatchProgressView;
import com.group4.javagrader.entity.Batch;

import java.util.Optional;

public interface BatchService {

    BatchPrecheckView buildPrecheck(Long assignmentId);

    Batch createBatch(Long assignmentId, String username);

    Batch startBatch(Long assignmentId, Long batchId, String username);

    Optional<Batch> findById(Long batchId);

    Optional<Batch> findLatestBatch(Long assignmentId);

    Optional<BatchProgressView> buildProgress(Long assignmentId, Long batchId);
}
