package com.group4.javagrader.service;

import com.group4.javagrader.dto.ResultDetailView;
import com.group4.javagrader.dto.ResultIndexView;

import java.util.List;
import java.util.Optional;

public interface ResultService {

    Optional<ResultIndexView> buildIndex(Long assignmentId);

    Optional<ResultDetailView> buildDetail(Long assignmentId, Long resultId);

    Optional<List<ResultDetailView>> buildDetailsForLatestBatch(Long assignmentId);
}
