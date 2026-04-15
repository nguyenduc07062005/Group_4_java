package com.group4.javagrader.entity;

import java.util.Locale;

public enum InputMode {
    STDIN,
    FILE;

    public boolean isFile() {
        return this == FILE;
    }

    public static InputMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return InputMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
