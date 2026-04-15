package com.group4.javagrader.repository;

import com.group4.javagrader.entity.PlagiarismReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlagiarismReportRepository extends JpaRepository<PlagiarismReport, Long> {

    Optional<PlagiarismReport> findFirstByAssignmentIdOrderByStartedAtDescIdDesc(Long assignmentId);

    List<PlagiarismReport> findByAssignmentIdInOrderByAssignmentIdAscStartedAtDescIdDesc(List<Long> assignmentIds);
}
