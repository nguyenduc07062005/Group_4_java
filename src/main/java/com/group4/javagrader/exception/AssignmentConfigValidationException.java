package com.group4.javagrader.exception;

public class AssignmentConfigValidationException extends InputValidationException {

    private final String fieldName;

    public AssignmentConfigValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
