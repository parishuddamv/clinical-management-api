package com.clinicos.patient.exception;

import com.clinicos.common.exception.ResourceNotFoundException;

/**
 * Exception thrown when a patient is not found
 */
public class PatientNotFoundException extends ResourceNotFoundException {

    public PatientNotFoundException(Long patientId) {
        super("Patient", "ID: " + patientId);
    }

    public PatientNotFoundException(String message) {
        super(message);
    }
}

