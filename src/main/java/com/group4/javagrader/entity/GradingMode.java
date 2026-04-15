package com.group4.javagrader.entity;

public enum GradingMode {
    JAVA_CORE,
    OOP;

    public boolean isJavaCore() {
        return this == JAVA_CORE;
    }

    public boolean isOop() {
        return this == OOP;
    }
}
