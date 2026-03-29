package com.group4.javagrader.service;

import com.group4.javagrader.dto.ProblemDetailDto;
import com.group4.javagrader.dto.ProblemForm;
import java.util.List;
import java.util.Optional;

public interface ProblemService {

    List<ProblemDetailDto> findByAssignmentId(Long assignmentId);

    Optional<ProblemDetailDto> findDetailById(Long id);

    Long create(Long assignmentId, ProblemForm form);

    boolean existsByAssignmentIdAndProblemOrder(Long assignmentId, Integer problemOrder);
}
