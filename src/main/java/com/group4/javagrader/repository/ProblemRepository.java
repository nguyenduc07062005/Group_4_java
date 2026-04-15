package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByAssignmentIdOrderByProblemOrderAsc(Long assignmentId);

    List<Problem> findByAssignmentIdAndInternalDefaultFalseOrderByProblemOrderAsc(Long assignmentId);

    List<Problem> findByAssignmentIdInAndInternalDefaultFalseOrderByAssignmentIdAscProblemOrderAsc(List<Long> assignmentIds);

    Optional<Problem> findFirstByAssignmentIdAndInternalDefaultTrueOrderByProblemOrderAsc(Long assignmentId);

    boolean existsByAssignmentIdAndInternalDefaultFalse(Long assignmentId);

    @Query("select coalesce(max(p.problemOrder), 0) from Problem p where p.assignment.id = :assignmentId")
    int findMaxProblemOrderByAssignmentId(@Param("assignmentId") Long assignmentId);
}
