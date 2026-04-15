CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(150) NOT NULL,
    week_count INT NOT NULL DEFAULT 0,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_courses_semester
        FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT uk_courses_semester_code
        UNIQUE (semester_id, course_code)
);

CREATE INDEX idx_courses_semester_id ON courses(semester_id);

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS course_id BIGINT NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS assignment_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOM';

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS week_number INT NULL;

ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 1000;

INSERT INTO courses (semester_id, course_code, course_name, week_count, archived)
SELECT s.id, 'GENERAL', 'Mon hoc chung', 0, FALSE
FROM semesters s
WHERE NOT EXISTS (
    SELECT 1
    FROM courses c
    WHERE c.semester_id = s.id
      AND c.course_code = 'GENERAL'
);

UPDATE assignments
SET course_id = (
    SELECT c.id
    FROM courses c
    WHERE c.semester_id = assignments.semester_id
      AND c.course_code = 'GENERAL'
)
WHERE course_id IS NULL;

ALTER TABLE assignments
    MODIFY COLUMN course_id BIGINT NOT NULL;

ALTER TABLE assignments
    ADD CONSTRAINT fk_assignments_course
        FOREIGN KEY (course_id) REFERENCES courses(id);

CREATE INDEX idx_assignments_course_id ON assignments(course_id);
