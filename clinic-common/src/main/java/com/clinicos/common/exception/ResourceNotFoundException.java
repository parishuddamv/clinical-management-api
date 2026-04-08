package com.clinicos.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ClinicOSException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found: %s", resourceName, identifier));
    }
}

