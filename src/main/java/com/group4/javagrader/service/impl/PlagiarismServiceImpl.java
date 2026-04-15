package com.group4.javagrader.service.impl;

import com.group4.javagrader.dto.BlockedSubmissionView;
import com.group4.javagrader.dto.PlagiarismDashboardView;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.PlagiarismPair;
import com.group4.javagrader.entity.PlagiarismReport;
import com.group4.javagrader.entity.Submission;
import com.group4.javagrader.grading.context.SubmissionFile;
import com.group4.javagrader.grading.plagiarism.NormalizedSubmissionSource;
import com.group4.javagrader.grading.plagiarism.PlagiarismComparison;
import com.group4.javagrader.grading.plagiarism.PlagiarismEngine;
import com.group4.javagrader.grading.plagiarism.SourceNormalizer;
import com.group4.javagrader.exception.InputValidationException;
import com.group4.javagrader.exception.OwnershipViolationException;
import com.group4.javagrader.exception.ResourceNotFoundException;
import com.group4.javagrader.exception.WorkflowStateException;
import com.group4.javagrader.repository.AssignmentRepository;
import com.group4.javagrader.repository.PlagiarismPairRepository;
import com.group4.javagrader.repository.PlagiarismReportRepository;
import com.group4.javagrader.repository.SubmissionRepository;
import com.group4.javagrader.service.PlagiarismService;
import com.group4.javagrader.storage.SubmissionStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class PlagiarismServiceImpl implements PlagiarismService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PlagiarismReportRepository plagiarismReportRepository;
    private final PlagiarismPairRepository plagiarismPairRepository;
    private final SubmissionStorageService submissionStorageService;
    private final SourceNormalizer sourceNormalizer;
    private final PlagiarismEngine plagiarismEngine;

    public PlagiarismServiceImpl(
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            PlagiarismReportRepository plagiarismReportRepository,
            PlagiarismPairRepository plagiarismPairRepository,
            SubmissionStorageService submissionStorageService,
            SourceNormalizer sourceNormalizer,
            PlagiarismEngine plagiarismEngine) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.plagiarismReportRepository = plagiarismReportRepository;
        this.plagiarismPairRepository = plagiarismPairRepository;
        this.submissionStorageService = submissionStorageService;
        this.sourceNormalizer = sourceNormalizer;
        this.plagiarismEngine = plagiarismEngine;
    }

    @Override
    @Transactional
    public PlagiarismReport runReport(Long assignmentId, String username) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        List<Submission> validatedSubmissions = loadValidatedSubmissionsForReport(assignmentId);

        PlagiarismReport report = new PlagiarismReport();
        report.setAssignment(assignment);
        report.setStatus("RUNNING");
        report.setThreshold(assignment.getPlagiarismThreshold());
        report.setStrategySummary(plagiarismEngine.describeWeights());
        report.setTotalSubmissions(validatedSubmissions.size());
        report.setFlaggedPairCount(0);
        report.setBlockedSubmissionCount(0);
        report.setRunBy(defaultUsername(username));
        report.setStartedAt(LocalDateTime.now());
        report = plagiarismReportRepository.save(report);

        Map<Long, Submission> submissionsById = new LinkedHashMap<>();
        List<NormalizedSubmissionSource> normalizedSources = new ArrayList<>();
        for (Submission submission : validatedSubmissions) {
            submissionsById.put(submission.getId(), submission);
            List<SubmissionFile> files = submissionStorageService.loadSubmissionFiles(submission.getStoragePath());
            normalizedSources.add(new NormalizedSubmissionSource(
                    submission.getId(),
                    submission.getSubmitterName(),
                    sourceNormalizer.normalize(files)));
        }

        List<PlagiarismPair> persistedPairs = persistPairs(report, assignment, submissionsById, normalizedSources);

        report.setStatus("COMPLETED");
        report.setCompletedAt(LocalDateTime.now());
        report.setFlaggedPairCount(countEffectivelyBlockedPairs(persistedPairs));
        report.setBlockedSubmissionCount(collectBlockedSubmissionIds(persistedPairs).size());
        return plagiarismReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlagiarismReport> findLatestReport(Long assignmentId) {
        return plagiarismReportRepository.findFirstByAssignmentIdOrderByStartedAtDescIdDesc(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlagiarismPair> findPairsByReportId(Long reportId) {
        return plagiarismPairRepository.findByReportIdOrderByFinalScoreDescIdAsc(reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public PlagiarismDashboardView buildDashboard(Long assignmentId) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        List<Submission> validatedSubmissions = loadValidatedSubmissionsForDashboard(assignmentId);
        PlagiarismReport latestReport = loadLatestReportOrNull(assignmentId);
        List<PlagiarismPair> pairs = loadPairsForReport(latestReport);

        return new PlagiarismDashboardView(
                assignment,
                validatedSubmissions,
                latestReport,
                pairs,
                buildBlockedSubmissionViews(pairs));
    }

    @Override
    @Transactional
    public PlagiarismPair overridePair(Long assignmentId, Long pairId, String decision, String note, String username) {
        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!"ALLOW".equals(normalizedDecision) && !"BLOCK".equals(normalizedDecision)) {
            throw new InputValidationException("Override decision must be ALLOW or BLOCK.");
        }

        PlagiarismReport latestReport = plagiarismReportRepository.findFirstByAssignmentIdOrderByStartedAtDescIdDesc(assignmentId)
                .orElseThrow(() -> new WorkflowStateException("No plagiarism report is available for this assignment."));

        PlagiarismPair pair = plagiarismPairRepository.findById(pairId)
                .orElseThrow(() -> new ResourceNotFoundException("Plagiarism pair not found."));
        if (!pair.getReport().getAssignment().getId().equals(assignmentId)) {
            throw new OwnershipViolationException("Plagiarism pair does not belong to this assignment.");
        }
        if (!pair.getReport().getId().equals(latestReport.getId())) {
            throw new WorkflowStateException("Plagiarism pair not found for the latest report of this assignment.");
        }

        pair.setOverrideDecision(normalizedDecision);
        pair.setOverrideNote(StringUtils.hasText(note) ? note.trim() : defaultOverrideNote(normalizedDecision));
        pair.setOverrideBy(defaultUsername(username));
        pair.setOverrideAt(LocalDateTime.now());

        PlagiarismPair savedPair = plagiarismPairRepository.save(pair);
        refreshReportMetrics(savedPair.getReport());
        return savedPair;
    }

    private void refreshReportMetrics(PlagiarismReport report) {
        refreshReportMetrics(report, loadPairsForReport(report));
    }

    private void refreshReportMetrics(PlagiarismReport report, List<PlagiarismPair> pairs) {
        report.setFlaggedPairCount(countEffectivelyBlockedPairs(pairs));
        report.setBlockedSubmissionCount(collectBlockedSubmissionIds(pairs).size());
        plagiarismReportRepository.save(report);
    }

    private Assignment loadAssignmentOrThrow(Long assignmentId) {
        return assignmentRepository.findByIdWithSemesterAndCourse(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
    }

    private List<Submission> loadValidatedSubmissionsForReport(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId).stream()
                .filter(Submission::isValidated)
                .sorted(Comparator.comparing(Submission::getId))
                .toList();
    }

    private List<Submission> loadValidatedSubmissionsForDashboard(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderByCreatedAtDescIdDesc(assignmentId).stream()
                .filter(Submission::isValidated)
                .sorted(Comparator.comparing(Submission::getSubmitterName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private PlagiarismReport loadLatestReportOrNull(Long assignmentId) {
        return findLatestReport(assignmentId).orElse(null);
    }

    private List<PlagiarismPair> loadPairsForReport(PlagiarismReport report) {
        if (report == null) {
            return List.of();
        }
        return plagiarismPairRepository.findByReportIdOrderByFinalScoreDescIdAsc(report.getId());
    }

    private List<PlagiarismPair> persistPairs(
            PlagiarismReport report,
            Assignment assignment,
            Map<Long, Submission> submissionsById,
            List<NormalizedSubmissionSource> normalizedSources) {
        List<PlagiarismPair> persistedPairs = new ArrayList<>();
        for (PlagiarismComparison comparison : plagiarismEngine.compareAll(normalizedSources)) {
            persistedPairs.add(savePair(report, assignment, submissionsById, comparison));
        }
        return persistedPairs;
    }

    private PlagiarismPair savePair(
            PlagiarismReport report,
            Assignment assignment,
            Map<Long, Submission> submissionsById,
            PlagiarismComparison comparison) {
        PlagiarismPair pair = new PlagiarismPair();
        pair.setReport(report);
        pair.setLeftSubmission(submissionsById.get(comparison.leftSubmissionId()));
        pair.setRightSubmission(submissionsById.get(comparison.rightSubmissionId()));
        pair.setTextScore(comparison.textScore());
        pair.setTokenScore(comparison.tokenScore());
        pair.setAstScore(comparison.astScore());
        pair.setFinalScore(comparison.finalScore());

        boolean blocked = comparison.finalScore().compareTo(assignment.getPlagiarismThreshold()) >= 0;
        pair.setBlocked(blocked);
        if (blocked) {
            pair.setReason("Similarity met or exceeded the assignment threshold of " + assignment.getPlagiarismThreshold() + "%.");
        } else {
            pair.setReason("Similarity stayed below the assignment threshold.");
        }

        return plagiarismPairRepository.save(pair);
    }

    private List<BlockedSubmissionView> buildBlockedSubmissionViews(List<PlagiarismPair> pairs) {
        Map<Long, BlockedSubmissionView> blockedBySubmission = new LinkedHashMap<>();
        for (PlagiarismPair pair : pairs) {
            if (!pair.isEffectivelyBlocked()) {
                continue;
            }

            registerBlockedSubmission(blockedBySubmission, pair.getLeftSubmission(), pair.getRightSubmission(), pair);
            registerBlockedSubmission(blockedBySubmission, pair.getRightSubmission(), pair.getLeftSubmission(), pair);
        }

        return blockedBySubmission.values().stream()
                .sorted(Comparator.comparing(BlockedSubmissionView::getScore).reversed())
                .toList();
    }

    private void registerBlockedSubmission(
            Map<Long, BlockedSubmissionView> blockedBySubmission,
            Submission blockedSubmission,
            Submission matchedSubmission,
            PlagiarismPair pair) {
        BlockedSubmissionView current = blockedBySubmission.get(blockedSubmission.getId());
        if (current == null || current.getScore().compareTo(pair.getFinalScore()) < 0) {
            blockedBySubmission.put(blockedSubmission.getId(), new BlockedSubmissionView(
                    blockedSubmission.getId(),
                    blockedSubmission.getSubmitterName(),
                    matchedSubmission.getSubmitterName(),
                    pair.getFinalScore(),
                    "Flagged with " + matchedSubmission.getSubmitterName() + " at " + pair.getFinalScore() + "%."));
        }
    }

    private LinkedHashSet<Long> collectBlockedSubmissionIds(List<PlagiarismPair> pairs) {
        LinkedHashSet<Long> blockedIds = new LinkedHashSet<>();
        for (PlagiarismPair pair : pairs) {
            if (!pair.isEffectivelyBlocked()) {
                continue;
            }
            blockedIds.add(pair.getLeftSubmission().getId());
            blockedIds.add(pair.getRightSubmission().getId());
        }
        return blockedIds;
    }

    private int countEffectivelyBlockedPairs(List<PlagiarismPair> pairs) {
        return (int) pairs.stream()
                .filter(PlagiarismPair::isEffectivelyBlocked)
                .count();
    }

    private String defaultUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "system";
    }

    private String defaultOverrideNote(String decision) {
        if ("ALLOW".equals(decision)) {
            return "Manual override released this pair from plagiarism blocking.";
        }
        return "Manual override re-applied plagiarism blocking for this pair.";
    }
}
