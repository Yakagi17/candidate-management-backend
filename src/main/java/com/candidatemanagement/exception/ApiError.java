package com.candidatemanagement.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ApiError {
    private HttpStatus status;
    private String title;
    private String message;
    private List<ValidationException.FieldError> details;
    private LocalDateTime timestamp;

    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(HttpStatus status, String title, String message) {
        this(status, title, message, List.of());
    }

    public ApiError(HttpStatus status, String title, String message, List<ValidationException.FieldError> details) {
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

    public List<ValidationException.FieldError> getDetails() {return details;}

    public void setDetails(List<ValidationException.FieldError> details) {this.details = details;}

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

