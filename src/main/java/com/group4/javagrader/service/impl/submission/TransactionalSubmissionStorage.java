package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

public final class TransactionalSubmissionStorage {

    private final SubmissionStorageService submissionStorageService;

    public TransactionalSubmissionStorage(SubmissionStorageService submissionStorageService) {
        this.submissionStorageService = submissionStorageService;
    }

    public void deleteSubmissionFilesAfterCommit(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteSubmissionFilesSafely(storagePath);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteSubmissionFilesSafely(storagePath);
            }
        });
    }

    public void deleteNewSubmissionFilesAfterRollback(String storagePath, String previousStoragePath) {
        if (StringUtils.hasText(previousStoragePath)
                || !StringUtils.hasText(storagePath)
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteSubmissionFilesSafely(storagePath);
                }
            }
        });
    }

    private void deleteSubmissionFilesSafely(String storagePath) {
        try {
            submissionStorageService.deleteSubmissionFiles(storagePath);
        } catch (IllegalArgumentException ignored) {
            // Cleanup should not mask the upload result after the database state has already settled.
        }
    }
}
