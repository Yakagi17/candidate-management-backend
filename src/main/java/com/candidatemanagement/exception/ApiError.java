package com.candidatemanagement.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class ApiError {
    private HttpStatus status;
    private String title;
    private String message;
    private ValidationException.FieldError[] details;
    private LocalDateTime timestamp;

    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(HttpStatus status, String title, String message) {
        this(status, title, message, new ValidationException.FieldError[]{});
    }

    public ApiError(HttpStatus status, String title, String message, ValidationException.FieldError[] details) {
        this();
        this.status = status;
        this.title = title;
        this.message = message;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ValidationException.FieldError[] getDetails() {return details;}

    public void setDetails(ValidationException.FieldError[] details) {this.details = details;}

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

