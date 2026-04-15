package com.group4.javagrader.repository;

import com.group4.javagrader.entity.GradingResult;
import com.group4.javagrader.entity.GradingResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradingResultRepository extends JpaRepository<GradingResult, Long> {

    List<GradingResult> findByBatchIdOrderBySubmissionSubmitterNameAscIdAsc(Long batchId);

    List<GradingResult> findBySubmissionAssignmentIdOrderBySubmissionSubmitterNameAscIdAsc(Long assignmentId);

    Optional<GradingResult> findFirstBySubmissionAssignmentIdOrderByIdDesc(Long assignmentId);

    Optional<GradingResult> findByBatchIdAndSubmissionId(Long batchId, Long submissionId);

    boolean existsBySubmissionId(Long submissionId);

    long countByBatchIdAndStatus(Long batchId, GradingResultStatus status);

    @Query("""
            select gr.batch.id as batchId,
                   count(gr) as totalCount,
                   sum(case when gr.status <> com.group4.javagrader.entity.GradingResultStatus.PENDING
                            and gr.status <> com.group4.javagrader.entity.GradingResultStatus.RUNNING then 1 else 0 end) as completedCount
            from GradingResult gr
            where gr.batch.id in :batchIds
            group by gr.batch.id
            """)
    List<BatchProgressStats> summarizeByBatchIds(@Param("batchIds") List<Long> batchIds);

    interface BatchProgressStats {
        Long getBatchId();

        long getTotalCount();

        long getCompletedCount();
    }
}
