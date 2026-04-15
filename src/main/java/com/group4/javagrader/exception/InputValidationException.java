package com.group4.javagrader.exception;

public class InputValidationException extends DomainException {

    public InputValidationException(String message) {
        super(message);
    }

    public InputValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
