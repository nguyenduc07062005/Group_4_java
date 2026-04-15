package com.group4.javagrader.semester;

import com.group4.javagrader.dto.CourseForm;
import com.group4.javagrader.entity.Semester;
import com.group4.javagrader.exception.AssignmentConfigValidationException;
import com.group4.javagrader.repository.SemesterRepository;
import com.group4.javagrader.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SemesterArchiveLockingIntegrationTest {

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void createCourseWaitsForArchivedSemesterCommitAndThenFails() throws Exception {
        Long semesterId = createSemester("LOCKSEM");

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Void> archiveFuture = executor.submit(() -> {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                transactionTemplate.executeWithoutResult(status -> {
                    Semester lockedSemester = semesterRepository.findByIdAndArchivedFalseForUpdate(semesterId).orElseThrow();
                    lockedSemester.setArchived(true);
                    lockHeld.countDown();
                    await(allowCommit);
                });
                return null;
            });

            assertThat(lockHeld.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Long> createFuture = executor.submit(() -> courseService.create(courseForm(semesterId, "SWE401")));

            Thread.sleep(250);
            assertThat(createFuture.isDone()).isFalse();

            allowCommit.countDown();
            archiveFuture.get(5, TimeUnit.SECONDS);

            assertThatThrownBy(() -> createFuture.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(AssignmentConfigValidationException.class)
                    .cause()
                    .hasMessage("Selected semester was not found.");
        } finally {
            executor.shutdownNow();
        }
    }

    private Long createSemester(String code) {
        Semester semester = new Semester();
        semester.setCode(code + "-" + System.nanoTime());
        semester.setName("Semester " + code);
        semester.setStartDate(LocalDate.of(2026, 1, 1));
        semester.setEndDate(LocalDate.of(2026, 5, 31));
        semester.setArchived(false);
        return semesterRepository.save(semester).getId();
    }

    private CourseForm courseForm(Long semesterId, String courseCode) {
        CourseForm form = new CourseForm();
        form.setSemesterId(semesterId);
        form.setCourseCode(courseCode);
        form.setCourseName("Locked Course");
        form.setWeekCount(0);
        form.setCreateWeeklyAssignments(false);
        return form;
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to release the archive transaction.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to release the archive transaction.", ex);
        }
    }
}
