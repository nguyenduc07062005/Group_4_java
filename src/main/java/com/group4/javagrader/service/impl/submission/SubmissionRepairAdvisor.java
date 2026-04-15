package com.group4.javagrader.service.impl.submission;

import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.grading.context.SubmissionFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SubmissionRepairAdvisor {

    private final SubmissionStructureRepairPlanner structureRepairPlanner;
    private final SubmissionValidationService validationService;

    public SubmissionRepairAdvisor(
            SubmissionStructureRepairPlanner structureRepairPlanner,
            SubmissionValidationService validationService) {
        this.structureRepairPlanner = structureRepairPlanner;
        this.validationService = validationService;
    }

    public boolean canBeRepairedFromPathsAlone(
            Assignment assignment,
            String submitterName,
            List<SubmissionFile> files) {
        return buildRepairCandidate(assignment, files)
                .filter(candidate -> !candidate.isEmpty())
                .map(candidate -> validationService.validate(assignment, submitterName, candidate).isEmpty())
                .orElse(false);
    }

    public Optional<List<SubmissionFile>> buildRepairCandidate(Assignment assignment, List<SubmissionFile> files) {
        return structureRepairPlanner.buildRepairCandidate(assignment, files);
    }

    public Map<String, String> buildSuggestedPaths(
            List<SubmissionFile> originalFiles,
            List<SubmissionFile> repairedFiles) {
        return structureRepairPlanner.buildSuggestedPaths(originalFiles, repairedFiles);
    }
}
