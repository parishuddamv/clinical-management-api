package com.clinicos.patient.exception;

import com.clinicos.common.exception.DuplicateResourceException;

/**
 * Exception thrown when attempting to register a patient with duplicate phone
 */
public class DuplicatePatientException extends DuplicateResourceException {

    public DuplicatePatientException(String phone) {
        super("Patient", "phone", phone);
    }
}

