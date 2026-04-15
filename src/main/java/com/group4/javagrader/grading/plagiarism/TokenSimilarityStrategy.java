package com.group4.javagrader.grading.plagiarism;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(20)
public class TokenSimilarityStrategy implements PlagiarismStrategy {

    private static final BigDecimal WEIGHT = new BigDecimal("0.75");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"[^\"]*\"|\\d+|[a-zA-Z_][a-zA-Z0-9_]*");
    private static final int TOKEN_WINDOW = 3;
    private static final Set<String> STOP_WORDS = Set.of(
            "public", "class", "static", "void", "main", "string", "args",
            "system", "out", "println", "print", "return", "new");

    @Override
    public String getCode() {
        return "TOKEN";
    }

    @Override
    public BigDecimal getWeight() {
        return WEIGHT;
    }

    @Override
    public BigDecimal compare(NormalizedSubmissionSource left, NormalizedSubmissionSource right) {
        Set<String> leftWindows = buildTokenWindows(left.normalizedSource());
        Set<String> rightWindows = buildTokenWindows(right.normalizedSource());

        if (leftWindows.isEmpty() || rightWindows.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (leftWindows.equals(rightWindows)) {
            return BigDecimal.valueOf(100);
        }

        Set<String> intersection = new LinkedHashSet<>(leftWindows);
        intersection.retainAll(rightWindows);

        Set<String> union = new LinkedHashSet<>(leftWindows);
        union.addAll(rightWindows);

        return BigDecimal.valueOf(intersection.size() * 100.0d / union.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Set<String> buildTokenWindows(String source) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(source);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (!STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }

        Set<String> windows = new LinkedHashSet<>();
        if (tokens.isEmpty()) {
            return windows;
        }
        if (tokens.size() < TOKEN_WINDOW) {
            windows.add(String.join(" ", tokens));
            return windows;
        }

        for (int i = 0; i <= tokens.size() - TOKEN_WINDOW; i++) {
            windows.add(String.join(" ", tokens.subList(i, i + TOKEN_WINDOW)));
        }
        return windows;
    }
}