package com.group4.javagrader.grading.plagiarism;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(30)
public class AstSimilarityStrategy implements PlagiarismStrategy {

    private static final BigDecimal WEIGHT = BigDecimal.ZERO;
    private static final Pattern STRUCTURE_PATTERN = Pattern.compile("\\b(class|interface|enum|record|if|for|while|switch|try|catch|return|new|public|private|protected|static|void)\\b");

    @Override
    public String getCode() {
        return "AST_SKELETON";
    }

    @Override
    public BigDecimal getWeight() {
        return WEIGHT;
    }

    @Override
    public BigDecimal compare(NormalizedSubmissionSource left, NormalizedSubmissionSource right) {
        Set<String> leftStructure = buildStructureWindows(left.normalizedSource());
        Set<String> rightStructure = buildStructureWindows(right.normalizedSource());

        if (leftStructure.isEmpty() || rightStructure.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        Set<String> intersection = new LinkedHashSet<>(leftStructure);
        intersection.retainAll(rightStructure);

        Set<String> union = new LinkedHashSet<>(leftStructure);
        union.addAll(rightStructure);

        return BigDecimal.valueOf(intersection.size() * 100.0d / union.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Set<String> buildStructureWindows(String source) {
        List<String> structures = new ArrayList<>();
        Matcher matcher = STRUCTURE_PATTERN.matcher(source);
        while (matcher.find()) {
            structures.add(matcher.group());
        }

        Set<String> windows = new LinkedHashSet<>();
        if (structures.isEmpty()) {
            return windows;
        }
        if (structures.size() == 1) {
            windows.add(structures.get(0));
            return windows;
        }

        for (int i = 0; i < structures.size() - 1; i++) {
            windows.add(structures.get(i) + " -> " + structures.get(i + 1));
        }
        return windows;
    }
}