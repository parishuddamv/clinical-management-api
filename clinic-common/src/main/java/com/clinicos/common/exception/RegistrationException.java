package com.clinicos.common.exception;

import org.springframework.http.HttpStatus;

/** Only client-safe messages belong in this exception. */
public class RegistrationException extends RuntimeException {
    private final HttpStatus status;

    public RegistrationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
