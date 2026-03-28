package com.group4.javagrader.service;

import com.group4.javagrader.dto.AssignmentForm;

public interface AssignmentService {
    Long create(AssignmentForm form);
}