package com.clinicos.patient.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Java time types are always serializable in this service.
 */
@Configuration
public class PatientJacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer patientJacksonCustomizer() {
        return builder -> builder
                .modulesToInstall(JavaTimeModule.class)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

