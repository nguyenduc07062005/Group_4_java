package com.group4.javagrader.repository;

import com.group4.javagrader.entity.OopRuleResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OopRuleResultRepository extends JpaRepository<OopRuleResult, Long> {

    List<OopRuleResult> findByGradingResultIdOrderByIdAsc(Long gradingResultId);

    List<OopRuleResult> findByGradingResultIdInOrderByGradingResultIdAscIdAsc(List<Long> gradingResultIds);
}
