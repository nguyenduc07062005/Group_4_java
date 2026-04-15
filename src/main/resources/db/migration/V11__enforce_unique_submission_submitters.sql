DROP TABLE IF EXISTS submission_dedup_map_v11;

CREATE TABLE submission_dedup_map_v11 (
    duplicate_id BIGINT PRIMARY KEY,
    keep_id BIGINT NOT NULL
);

INSERT INTO submission_dedup_map_v11 (duplicate_id, keep_id)
SELECT duplicate_submission.id, kept_submission.keep_id
FROM submissions duplicate_submission
JOIN (
    SELECT assignment_id,
           LOWER(TRIM(submitter_name)) AS normalized_submitter_name,
           MAX(id) AS keep_id
    FROM submissions
    GROUP BY assignment_id, LOWER(TRIM(submitter_name))
    HAVING COUNT(*) > 1
) kept_submission
    ON kept_submission.assignment_id = duplicate_submission.assignment_id
    AND kept_submission.normalized_submitter_name = LOWER(TRIM(duplicate_submission.submitter_name))
WHERE duplicate_submission.id <> kept_submission.keep_id;

DELETE FROM test_case_results
WHERE problem_result_id IN (
    SELECT id
    FROM (
        SELECT problem_result.id
        FROM problem_results problem_result
        WHERE problem_result.grading_result_id IN (
            SELECT id
            FROM (
                SELECT duplicate_result.id
                FROM grading_results duplicate_result
                JOIN submission_dedup_map_v11 dedup_map
                    ON dedup_map.duplicate_id = duplicate_result.submission_id
                JOIN grading_results kept_result
                    ON kept_result.batch_id = duplicate_result.batch_id
                    AND kept_result.submission_id = dedup_map.keep_id
            ) stale_grading_results
        )
    ) stale_problem_results
);

DELETE FROM problem_results
WHERE grading_result_id IN (
    SELECT id
    FROM (
        SELECT duplicate_result.id
        FROM grading_results duplicate_result
        JOIN submission_dedup_map_v11 dedup_map
            ON dedup_map.duplicate_id = duplicate_result.submission_id
        JOIN grading_results kept_result
            ON kept_result.batch_id = duplicate_result.batch_id
            AND kept_result.submission_id = dedup_map.keep_id
    ) stale_grading_results
);

DELETE FROM oop_rule_results
WHERE grading_result_id IN (
    SELECT id
    FROM (
        SELECT duplicate_result.id
        FROM grading_results duplicate_result
        JOIN submission_dedup_map_v11 dedup_map
            ON dedup_map.duplicate_id = duplicate_result.submission_id
        JOIN grading_results kept_result
            ON kept_result.batch_id = duplicate_result.batch_id
            AND kept_result.submission_id = dedup_map.keep_id
    ) stale_grading_results
);

DELETE FROM grading_results
WHERE id IN (
    SELECT id
    FROM (
        SELECT duplicate_result.id
        FROM grading_results duplicate_result
        JOIN submission_dedup_map_v11 dedup_map
            ON dedup_map.duplicate_id = duplicate_result.submission_id
        JOIN grading_results kept_result
            ON kept_result.batch_id = duplicate_result.batch_id
            AND kept_result.submission_id = dedup_map.keep_id
    ) stale_grading_results
);

UPDATE grading_results
SET submission_id = (
    SELECT dedup_map.keep_id
    FROM submission_dedup_map_v11 dedup_map
    WHERE dedup_map.duplicate_id = grading_results.submission_id
)
WHERE submission_id IN (
    SELECT duplicate_id
    FROM submission_dedup_map_v11
);

UPDATE plagiarism_pairs
SET left_submission_id = (
    SELECT dedup_map.keep_id
    FROM submission_dedup_map_v11 dedup_map
    WHERE dedup_map.duplicate_id = plagiarism_pairs.left_submission_id
)
WHERE left_submission_id IN (
    SELECT duplicate_id
    FROM submission_dedup_map_v11
);

UPDATE plagiarism_pairs
SET right_submission_id = (
    SELECT dedup_map.keep_id
    FROM submission_dedup_map_v11 dedup_map
    WHERE dedup_map.duplicate_id = plagiarism_pairs.right_submission_id
)
WHERE right_submission_id IN (
    SELECT duplicate_id
    FROM submission_dedup_map_v11
);

DELETE FROM submissions
WHERE id IN (
    SELECT duplicate_id
    FROM submission_dedup_map_v11
);

DROP TABLE submission_dedup_map_v11;

ALTER TABLE submissions
    ADD CONSTRAINT uk_submissions_assignment_submitter UNIQUE (assignment_id, submitter_name);
