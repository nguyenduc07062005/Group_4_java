package com.group4.javagrader.service;

import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.entity.Assignment;

import java.util.Optional;

public interface AssignmentService {

    Long create(AssignmentForm form);

    Optional<Assignment> findById(Long id);
}
