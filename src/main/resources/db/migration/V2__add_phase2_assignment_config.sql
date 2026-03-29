ALTER TABLE assignments
ADD COLUMN output_normalization_policy VARCHAR(50) NOT NULL DEFAULT 'STRICT',
ADD COLUMN logic_weight INT NOT NULL DEFAULT 50,
ADD COLUMN oop_weight INT NOT NULL DEFAULT 50,
ADD COLUMN description_file_name VARCHAR(255) NULL,
ADD COLUMN description_file_content_type VARCHAR(100) NULL,
ADD COLUMN description_file_data LONGBLOB NULL,
ADD COLUMN oop_rule_config_file_name VARCHAR(255) NULL,
ADD COLUMN oop_rule_config_content_type VARCHAR(100) NULL,
ADD COLUMN oop_rule_config_data LONGBLOB NULL;
