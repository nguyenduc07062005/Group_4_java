package com.group4.javagrader.repository;

import com.group4.javagrader.entity.TestCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestCaseResultRepository extends JpaRepository<TestCaseResult, Long> {

    List<TestCaseResult> findByProblemResultIdOrderByTestCaseCaseOrderAscIdAsc(Long problemResultId);

    List<TestCaseResult> findByProblemResultIdInOrderByProblemResultIdAscTestCaseCaseOrderAscIdAsc(List<Long> problemResultIds);

    @Query("""
            select count(testCaseResult) > 0
            from TestCaseResult testCaseResult
            where testCaseResult.testCase.id = :testCaseId
            """)
    boolean existsByTestCaseId(@Param("testCaseId") Long testCaseId);
}
