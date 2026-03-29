package com.group4.javagrader.service;

import com.group4.javagrader.dto.TestCaseForm;
import com.group4.javagrader.dto.TestCaseSummaryDto;
import java.util.List;

public interface TestCaseService {

    List<TestCaseSummaryDto> findByProblemId(Long problemId);

    Long create(Long problemId, TestCaseForm form);

    boolean existsByProblemIdAndCaseOrder(Long problemId, Integer caseOrder);
}
