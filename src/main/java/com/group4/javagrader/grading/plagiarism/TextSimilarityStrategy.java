package com.group4.javagrader.grading.plagiarism;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@Order(10)
public class TextSimilarityStrategy implements PlagiarismStrategy {

    private static final BigDecimal WEIGHT = new BigDecimal("0.25");
    private static final int SHINGLE_SIZE = 12;

    @Override
    public String getCode() {
        return "TEXT";
    }

    @Override
    public BigDecimal getWeight() {
        return WEIGHT;
    }

    @Override
    public BigDecimal compare(NormalizedSubmissionSource left, NormalizedSubmissionSource right) {
        if (left.normalizedSource().equals(right.normalizedSource())) {
            return BigDecimal.valueOf(100);
        }

        Set<String> leftShingles = buildShingles(left.normalizedSource());
        Set<String> rightShingles = buildShingles(right.normalizedSource());
        if (leftShingles.isEmpty() || rightShingles.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        Set<String> intersection = new LinkedHashSet<>(leftShingles);
        intersection.retainAll(rightShingles);

        Set<String> union = new LinkedHashSet<>(leftShingles);
        union.addAll(rightShingles);

        return BigDecimal.valueOf(intersection.size() * 100.0d / union.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Set<String> buildShingles(String source) {
        Set<String> shingles = new LinkedHashSet<>();
        if (source.length() <= SHINGLE_SIZE) {
            if (!source.isBlank()) {
                shingles.add(source);
            }
            return shingles;
        }

        for (int i = 0; i <= source.length() - SHINGLE_SIZE; i++) {
            shingles.add(source.substring(i, i + SHINGLE_SIZE));
        }
        return shingles;
    }
}