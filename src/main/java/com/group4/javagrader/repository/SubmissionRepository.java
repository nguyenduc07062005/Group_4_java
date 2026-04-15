package com.group4.javagrader.repository;

import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentIdOrderByCreatedAtDescIdDesc(Long assignmentId);

    List<Submission> findByAssignmentIdAndSubmitterNameIgnoreCaseOrderByIdDesc(Long assignmentId, String submitterName);

    boolean existsByAssignmentId(Long assignmentId);

    long countByAssignmentIdAndStatus(Long assignmentId, SubmissionStatus status);

    @Query("""
            select s.assignment.id as assignmentId,
                   count(s) as submissionCount,
                   sum(case when s.status = com.group4.javagrader.entity.SubmissionStatus.VALIDATED then 1 else 0 end) as validatedCount,
                   sum(case when s.status = com.group4.javagrader.entity.SubmissionStatus.REJECTED then 1 else 0 end) as rejectedCount,
                   max(coalesce(s.updatedAt, s.createdAt)) as latestSubmissionAt
            from Submission s
            where s.assignment.id in :assignmentIds
            group by s.assignment.id
            """)
    List<AssignmentSubmissionStats> summarizeByAssignmentIds(@Param("assignmentIds") List<Long> assignmentIds);

    interface AssignmentSubmissionStats {
        Long getAssignmentId();

        long getSubmissionCount();

        long getValidatedCount();

        long getRejectedCount();

        LocalDateTime getLatestSubmissionAt();
    }
}
