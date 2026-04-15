package com.group4.javagrader.repository;

import com.group4.javagrader.entity.ProblemResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemResultRepository extends JpaRepository<ProblemResult, Long> {

    boolean existsByProblemId(Long problemId);

    List<ProblemResult> findByGradingResultIdOrderByProblemProblemOrderAscIdAsc(Long gradingResultId);

    List<ProblemResult> findByGradingResultIdInOrderByGradingResultIdAscProblemProblemOrderAscIdAsc(List<Long> gradingResultIds);
}
