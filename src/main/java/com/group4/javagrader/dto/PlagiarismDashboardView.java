package com.group4.javagrader.dto;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.PlagiarismPair;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Submission;
import lombok.Value;

import java.util.List;

@Value
public class PlagiarismDashboardView {

    Assignment assignment;
    List<Submission> validatedSubmissions;
    PlagiarismReport latestReport;
    List<PlagiarismPair> pairs;
    List<BlockedSubmissionView> blockedSubmissions;

    public int validatedSubmissionCount() {
        return validatedSubmissions.size();
    }

    public boolean hasReport() {
        return latestReport != null;
    }

    public boolean canRunReport() {
        return validatedSubmissionCount() >= 2;
    }

    public boolean canProceedToBatchPrecheck() {
        return latestReport != null
                && latestReport.isCompleted()
                && blockedSubmissions.isEmpty();
    }
}
