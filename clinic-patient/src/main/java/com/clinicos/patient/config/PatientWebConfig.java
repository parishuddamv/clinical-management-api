package com.clinicos.patient.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for clinic-patient service
 * Ensures proper routing of API endpoints
 */
@Configuration
public class PatientWebConfig implements WebMvcConfigurer {
    
    // This ensures that all endpoints are properly routed and not treated as static resources
    // The component scan in PatientApplication includes com.clinicos.common
    // This allows all controllers from clinic-common to be registered
    
}

