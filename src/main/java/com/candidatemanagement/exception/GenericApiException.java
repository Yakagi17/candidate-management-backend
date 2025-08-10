package com.candidatemanagement.exception;

import org.springframework.http.HttpStatus;

public class GenericApiException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final String message;

    public GenericApiException(HttpStatus status, String title, String message) {
        super(message);
        this.status = status;
        this.title = title;
        this.message = message;
    }

    public GenericApiException(HttpStatus status, String title, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.title = title;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

