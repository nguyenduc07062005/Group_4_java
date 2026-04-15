package com.group4.javagrader.entity;

import java.util.Locale;

public enum AssignmentType {
    INTRO,
    DEFAULT,
    WEEKLY,
    CUSTOM;

    public boolean isWeekly() {
        return this == WEEKLY;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    public static AssignmentType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AssignmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
