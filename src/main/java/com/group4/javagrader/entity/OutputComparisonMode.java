package com.group4.javagrader.entity;

import java.util.Locale;

public enum OutputComparisonMode {
    EXACT(0),
    TRIM_ALL(1),
    IGNORE_WHITESPACE(2);

    private final int normalizationRank;

    OutputComparisonMode(int normalizationRank) {
        this.normalizationRank = normalizationRank;
    }

    public int getNormalizationRank() {
        return normalizationRank;
    }

    public boolean isTrimAll() {
        return this == TRIM_ALL;
    }

    public boolean isIgnoreWhitespace() {
        return this == IGNORE_WHITESPACE;
    }

    public static OutputComparisonMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("STRICT".equals(normalized)) {
            return EXACT;
        }
        return OutputComparisonMode.valueOf(normalized);
    }

    public static OutputComparisonMode fromAssignmentPolicy(OutputNormalizationPolicy policy) {
        if (policy == null) {
            return EXACT;
        }
        return switch (policy) {
            case TRIM_ALL -> TRIM_ALL;
            case IGNORE_WHITESPACE -> IGNORE_WHITESPACE;
            default -> EXACT;
        };
    }
}
