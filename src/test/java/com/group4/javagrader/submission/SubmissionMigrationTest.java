package com.group4.javagrader.submission;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionMigrationTest {

    @Test
    void v11DeduplicatesExistingSubmitterRowsBeforeAddingUniqueConstraint() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                "");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("10")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        seedDuplicateSubmissions(jdbcTemplate);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("11")
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from submissions where assignment_id = 1 and submitter_name = 's2213001'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select submission_id from grading_results where id = 1",
                Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "select left_submission_id from plagiarism_pairs where id = 1",
                Long.class)).isEqualTo(2L);
    }

    private void seedDuplicateSubmissions(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                insert into semesters (id, code, name, start_date, end_date, archived)
                values (1, 'SP26-MIG', 'Spring 2026', '2026-01-06', '2026-05-30', false)
                """);
        jdbcTemplate.update("""
                insert into courses (id, semester_id, course_code, course_name, week_count, archived)
                values (1, 1, 'GENERAL', 'General', 0, false)
                """);
        jdbcTemplate.update("""
                insert into assignments (
                    id, semester_id, course_id, title, grading_mode, total_score, plagiarism_threshold,
                    output_normalization_policy, logic_weight, oop_weight, assignment_type, display_order)
                values (1, 1, 1, 'Migration Lab', 'JAVA_CORE', 100.00, 80.00, 'STRICT', 100, 0, 'CUSTOM', 1000)
                """);
        jdbcTemplate.update("""
                insert into submissions (
                    id, assignment_id, submitter_name, archive_file_name, status, file_count, validation_message)
                values (1, 1, 's2213001', 'old.zip', 'VALIDATED', 1, 'old')
                """);
        jdbcTemplate.update("""
                insert into submissions (
                    id, assignment_id, submitter_name, archive_file_name, status, file_count, validation_message)
                values (2, 1, 's2213001', 'new.zip', 'VALIDATED', 1, 'new')
                """);
        jdbcTemplate.update("""
                insert into submissions (
                    id, assignment_id, submitter_name, archive_file_name, status, file_count, validation_message)
                values (3, 1, 's2213002', 'other.zip', 'VALIDATED', 1, 'other')
                """);
        jdbcTemplate.update("""
                insert into plagiarism_reports (
                    id, assignment_id, status, threshold, total_submissions, flagged_pair_count, blocked_submission_count)
                values (1, 1, 'COMPLETED', 80.00, 3, 1, 2)
                """);
        jdbcTemplate.update("""
                insert into plagiarism_pairs (
                    id, report_id, left_submission_id, right_submission_id,
                    text_score, token_score, ast_score, final_score, blocked)
                values (1, 1, 1, 3, 90.00, 90.00, 90.00, 90.00, true)
                """);
        jdbcTemplate.update("""
                insert into batches (
                    id, assignment_id, plagiarism_report_id, status, queue_capacity, worker_count,
                    total_submissions, gradeable_submission_count, excluded_submission_count)
                values (1, 1, 1, 'COMPLETED', 8, 2, 2, 2, 0)
                """);
        jdbcTemplate.update("""
                insert into grading_results (
                    id, batch_id, submission_id, status, grading_mode,
                    total_score, max_score, logic_score, oop_score, testcase_passed_count, testcase_total_count)
                values (1, 1, 1, 'DONE', 'JAVA_CORE', 10.00, 10.00, 10.00, 0.00, 1, 1)
                """);
    }
}
