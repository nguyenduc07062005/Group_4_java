ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP NULL;

ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS error_summary TEXT NULL;

CREATE TABLE IF NOT EXISTS grading_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    grading_mode VARCHAR(30) NOT NULL,
    total_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    max_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    logic_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    oop_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    testcase_passed_count INT NOT NULL DEFAULT 0,
    testcase_total_count INT NOT NULL DEFAULT 0,
    compile_log TEXT NULL,
    execution_log LONGTEXT NULL,
    violation_summary TEXT NULL,
    artifact_path VARCHAR(500) NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_grading_results_batch_id (batch_id),
    INDEX idx_grading_results_assignment_status (status, batch_id),
    CONSTRAINT fk_grading_results_batch
        FOREIGN KEY (batch_id) REFERENCES batches(id),
    CONSTRAINT fk_grading_results_submission
        FOREIGN KEY (submission_id) REFERENCES submissions(id),
    CONSTRAINT uk_grading_results_batch_submission
        UNIQUE (batch_id, submission_id)
);

CREATE TABLE IF NOT EXISTS problem_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    grading_result_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    earned_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    max_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    testcase_passed_count INT NOT NULL DEFAULT 0,
    testcase_total_count INT NOT NULL DEFAULT 0,
    detail_summary TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_problem_results_grading_result_id (grading_result_id),
    CONSTRAINT fk_problem_results_grading_result
        FOREIGN KEY (grading_result_id) REFERENCES grading_results(id),
    CONSTRAINT fk_problem_results_problem
        FOREIGN KEY (problem_id) REFERENCES problems(id)
);

CREATE TABLE IF NOT EXISTS test_case_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_result_id BIGINT NOT NULL,
    test_case_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    earned_score DECIMAL(8,2) NOT NULL DEFAULT 0,
    configured_weight DECIMAL(8,2) NOT NULL DEFAULT 0,
    expected_output TEXT NULL,
    actual_output TEXT NULL,
    message TEXT NULL,
    runtime_millis BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_test_case_results_problem_result_id (problem_result_id),
    CONSTRAINT fk_test_case_results_problem_result
        FOREIGN KEY (problem_result_id) REFERENCES problem_results(id),
    CONSTRAINT fk_test_case_results_test_case
        FOREIGN KEY (test_case_id) REFERENCES test_cases(id)
);

CREATE TABLE IF NOT EXISTS oop_rule_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    grading_result_id BIGINT NOT NULL,
    rule_label VARCHAR(150) NOT NULL,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_oop_rule_results_grading_result_id (grading_result_id),
    CONSTRAINT fk_oop_rule_results_grading_result
        FOREIGN KEY (grading_result_id) REFERENCES grading_results(id)
);
