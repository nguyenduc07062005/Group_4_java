package com.group4.javagrader.entity;

import java.util.Locale;

public enum OutputNormalizationPolicy {
    STRICT,
    TRIM_ALL,
    IGNORE_WHITESPACE;

    public OutputComparisonMode toComparisonMode() {
        return OutputComparisonMode.fromAssignmentPolicy(this);
    }

    public static OutputNormalizationPolicy from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OutputNormalizationPolicy.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
