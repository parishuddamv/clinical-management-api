package com.clinicos.patient.config;

import com.clinicos.patient.dto.PatientSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PatientJacksonConfigTest {

    @Test
    void shouldSerializeLocalDateTimeInPatientSummaryResponse() throws Exception {
        PatientJacksonConfig config = new PatientJacksonConfig();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        config.patientJacksonCustomizer().customize(builder);
        // Keep parity with Spring Boot's module discovery in the actual app.
        builder.findModulesViaServiceLoader(true);
        ObjectMapper objectMapper = builder.build();

        PatientSummaryResponse dto = PatientSummaryResponse.builder()
                .id(1L)
                .firstName("Asha")
                .lastName("Patel")
                .phone("9999999999")
                .gender("FEMALE")
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 8, 25, 10, 30, 15))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"createdAt\":\"2026-08-25T10:30:15\"");
    }
}
