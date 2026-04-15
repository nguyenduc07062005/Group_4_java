CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    submitter_name VARCHAR(150) NOT NULL,
    archive_file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    validation_code VARCHAR(100) NULL,
    validation_message TEXT NULL,
    file_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_submissions_assignment_id (assignment_id),
    INDEX idx_submissions_assignment_status (assignment_id, status),
    CONSTRAINT fk_submissions_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id)
);
