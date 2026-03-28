package com.group4.javagrader.service;

import com.group4.javagrader.dto.ProblemForm;

public interface ProblemService {
    Long create(ProblemForm form);
}