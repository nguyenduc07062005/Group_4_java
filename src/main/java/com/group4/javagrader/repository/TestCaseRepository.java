package com.group4.javagrader.repository;

import com.group4.javagrader.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemIdOrderByCaseOrderAsc(Long problemId);

    long countByProblemId(Long problemId);

    @Query("select coalesce(max(tc.caseOrder), 0) from TestCase tc where tc.problem.id = :problemId")
    int findMaxCaseOrderByProblemId(@Param("problemId") Long problemId);

    @Query("""
            select tc.problem.id as problemId, count(tc) as testcaseCount
            from TestCase tc
            where tc.problem.id in :problemIds
            group by tc.problem.id
            """)
    List<ProblemTestCaseCount> summarizeByProblemIds(@Param("problemIds") List<Long> problemIds);

    interface ProblemTestCaseCount {
        Long getProblemId();

        long getTestcaseCount();
    }
}
