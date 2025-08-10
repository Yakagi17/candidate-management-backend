package com.candidatemanagement.exception;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public record FieldError(String field, String message) {
    }
}

