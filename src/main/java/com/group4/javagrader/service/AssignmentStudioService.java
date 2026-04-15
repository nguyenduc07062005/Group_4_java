package com.group4.javagrader.service;

import com.group4.javagrader.dto.AssignmentStudioView;

import java.util.Optional;

public interface AssignmentStudioService {

    Optional<AssignmentStudioView> build(Long assignmentId, Long selectedProblemId);
}
