ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS output_normalization_policy VARCHAR(50) NOT NULL DEFAULT 'STRICT';

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS logic_weight INT NOT NULL DEFAULT 50;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS oop_weight INT NOT NULL DEFAULT 50;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS description_file_name VARCHAR(255) NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS description_file_content_type VARCHAR(100) NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS description_file_data LONGBLOB NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS oop_rule_config_file_name VARCHAR(255) NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS oop_rule_config_content_type VARCHAR(100) NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS oop_rule_config_data LONGBLOB NULL;
