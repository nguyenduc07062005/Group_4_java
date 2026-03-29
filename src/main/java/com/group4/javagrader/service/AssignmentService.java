package com.group4.javagrader.service;

import com.group4.javagrader.dto.AssignmentDetailDto;
import com.group4.javagrader.dto.AssignmentForm;
import com.group4.javagrader.dto.AssignmentSummaryDto;
import java.util.List;
import java.util.Optional;

public interface AssignmentService {

    List<AssignmentSummaryDto> findAllSummaries();

    List<AssignmentSummaryDto> findBySemesterId(Long semesterId);

    Optional<AssignmentDetailDto> findDetailById(Long id);

    Long create(AssignmentForm form);

    boolean existsById(Long id);
}
