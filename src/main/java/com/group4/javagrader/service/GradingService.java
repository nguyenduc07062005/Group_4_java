package com.group4.javagrader.service;

import java.util.List;

public interface GradingService {

    void processBatch(Long batchId, List<Long> gradeableSubmissionIds);
}
