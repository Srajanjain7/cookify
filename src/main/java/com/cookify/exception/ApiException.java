package com.cookify.exception;

import org.springframework.http.HttpStatus;

/**
 * Carries one of the assignment pseudocode's literal error/status
 * messages (e.g. "This Username is taken, try again") through to the
 * JSON response with the right HTTP status.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
