package com.group4.javagrader.service;

import com.group4.javagrader.dto.GradingWorkspaceView;

import java.util.Optional;

public interface GradingWorkspaceService {

    Optional<GradingWorkspaceView> build(Long assignmentId, Long selectedResultId);
}
