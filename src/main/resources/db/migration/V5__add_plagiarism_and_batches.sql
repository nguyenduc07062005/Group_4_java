CREATE TABLE IF NOT EXISTS plagiarism_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    threshold DECIMAL(5,2) NOT NULL,
    strategy_summary TEXT NULL,
    total_submissions INT NOT NULL DEFAULT 0,
    flagged_pair_count INT NOT NULL DEFAULT 0,
    blocked_submission_count INT NOT NULL DEFAULT 0,
    run_by VARCHAR(100) NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plagiarism_reports_assignment_id (assignment_id),
    CONSTRAINT fk_plagiarism_reports_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id)
);

CREATE TABLE IF NOT EXISTS plagiarism_pairs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    left_submission_id BIGINT NOT NULL,
    right_submission_id BIGINT NOT NULL,
    text_score DECIMAL(5,2) NOT NULL,
    token_score DECIMAL(5,2) NOT NULL,
    ast_score DECIMAL(5,2) NOT NULL,
    final_score DECIMAL(5,2) NOT NULL,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    reason TEXT NULL,
    override_decision VARCHAR(20) NULL,
    override_note TEXT NULL,
    override_by VARCHAR(100) NULL,
    override_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plagiarism_pairs_report_id (report_id),
    INDEX idx_plagiarism_pairs_score (report_id, final_score),
    CONSTRAINT fk_plagiarism_pairs_report
        FOREIGN KEY (report_id) REFERENCES plagiarism_reports(id),
    CONSTRAINT fk_plagiarism_pairs_left_submission
        FOREIGN KEY (left_submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_plagiarism_pairs_right_submission
        FOREIGN KEY (right_submission_id) REFERENCES submissions(id)
);

CREATE TABLE IF NOT EXISTS batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    plagiarism_report_id BIGINT NULL,
    status VARCHAR(40) NOT NULL,
    queue_capacity INT NOT NULL,
    worker_count INT NOT NULL,
    total_submissions INT NOT NULL DEFAULT 0,
    gradeable_submission_count INT NOT NULL DEFAULT 0,
    excluded_submission_count INT NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NULL,
    started_by VARCHAR(100) NULL,
    precheck_summary TEXT NULL,
    started_at TIMESTAMP NULL,
    ready_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batches_assignment_id (assignment_id),
    INDEX idx_batches_status (assignment_id, status),
    CONSTRAINT fk_batches_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    CONSTRAINT fk_batches_plagiarism_report
        FOREIGN KEY (plagiarism_report_id) REFERENCES plagiarism_reports(id)
);
