package com.clinicos.common.exception;

/**
 * Base exception for all clinic-specific business logic errors.
 */
public class ClinicOSException extends RuntimeException {

    private final String errorCode;

    public ClinicOSException(String message) {
        super(message);
        this.errorCode = "CLINICOS_ERROR";
    }

    public ClinicOSException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClinicOSException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

