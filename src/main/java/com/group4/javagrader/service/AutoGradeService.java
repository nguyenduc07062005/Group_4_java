package com.group4.javagrader.service;

import com.group4.javagrader.dto.AutoGradeResult;

public interface AutoGradeService {

    AutoGradeResult run(Long assignmentId, String username);
}
