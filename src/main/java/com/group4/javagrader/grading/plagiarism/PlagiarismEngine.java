package com.group4.javagrader.grading.plagiarism;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlagiarismEngine {

    private final List<PlagiarismStrategy> strategies;

    public PlagiarismEngine(List<PlagiarismStrategy> strategies) {
        List<PlagiarismStrategy> orderedStrategies = new ArrayList<>(strategies);
        AnnotationAwareOrderComparator.sort(orderedStrategies);
        this.strategies = List.copyOf(orderedStrategies);
    }

    public List<PlagiarismComparison> compareAll(List<NormalizedSubmissionSource> sources) {
        List<PlagiarismComparison> comparisons = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < sources.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < sources.size(); rightIndex++) {
                NormalizedSubmissionSource left = sources.get(leftIndex);
                NormalizedSubmissionSource right = sources.get(rightIndex);

                BigDecimal textScore = findScore("TEXT", left, right);
                BigDecimal tokenScore = findScore("TOKEN", left, right);
                BigDecimal astScore = findScore("AST_SKELETON", left, right);

                BigDecimal finalScore = textScore.multiply(weightOf("TEXT"))
                        .add(tokenScore.multiply(weightOf("TOKEN")))
                        .add(astScore.multiply(weightOf("AST_SKELETON")))
                        .setScale(2, RoundingMode.HALF_UP);

                comparisons.add(new PlagiarismComparison(
                        left.submissionId(),
                        right.submissionId(),
                        textScore,
                        tokenScore,
                        astScore,
                        finalScore));
            }
        }
        return comparisons;
    }

    public String describeWeights() {
        return strategies.stream()
                .map(strategy -> strategy.getCode() + "=" + strategy.getWeight().movePointRight(2).setScale(0, RoundingMode.HALF_UP) + "%")
                .collect(Collectors.joining(", "));
    }

    private BigDecimal findScore(String code, NormalizedSubmissionSource left, NormalizedSubmissionSource right) {
        return strategies.stream()
                .filter(strategy -> strategy.getCode().equals(code))
                .findFirst()
                .map(strategy -> strategy.compare(left, right))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal weightOf(String code) {
        return strategies.stream()
                .filter(strategy -> strategy.getCode().equals(code))
                .findFirst()
                .map(PlagiarismStrategy::getWeight)
                .orElse(BigDecimal.ZERO);
    }
}
