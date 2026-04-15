package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Batch;
import com.group4.javagrader.entity.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findFirstByAssignmentIdOrderByIdDesc(Long assignmentId);

    List<Batch> findByAssignmentIdInOrderByAssignmentIdAscIdDesc(List<Long> assignmentIds);

    List<Batch> findByAssignmentIdAndStatusNotInOrderByIdDesc(Long assignmentId, List<BatchStatus> statuses);

    Optional<Batch> findFirstByAssignmentIdAndStatusNotOrderByIdDesc(Long assignmentId, BatchStatus status);

    @Query(value = "SELECT * FROM batches WHERE id = :batchId AND assignment_id = :assignmentId FOR UPDATE", nativeQuery = true)
    Optional<Batch> findByIdAndAssignmentIdForUpdate(@Param("batchId") Long batchId, @Param("assignmentId") Long assignmentId);

    boolean existsByAssignmentId(Long assignmentId);
}
