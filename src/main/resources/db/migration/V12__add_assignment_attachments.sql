CREATE TABLE IF NOT EXISTS assignment_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    attachment_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NULL,
    data LONGBLOB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_attachments_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    CONSTRAINT uk_assignment_attachments_assignment_type
        UNIQUE (assignment_id, attachment_type)
);

INSERT INTO assignment_attachments (assignment_id, attachment_type, file_name, content_type, data)
SELECT id, 'DESCRIPTION', description_file_name, description_file_content_type, description_file_data
FROM assignments
WHERE description_file_data IS NOT NULL;

INSERT INTO assignment_attachments (assignment_id, attachment_type, file_name, content_type, data)
SELECT id, 'OOP_RULE_CONFIG', oop_rule_config_file_name, oop_rule_config_content_type, oop_rule_config_data
FROM assignments
WHERE oop_rule_config_data IS NOT NULL;

CREATE INDEX idx_assignment_attachments_assignment_id ON assignment_attachments(assignment_id);
