package com.clinicos.common.exception;

/**
 * Business access denial for tenant/role/subscription authorization failures.
 */
public class ClinicAccessDeniedException extends ClinicOSException {

    public ClinicAccessDeniedException(String message) {
        super("ACCESS_DENIED", message);
    }
}

