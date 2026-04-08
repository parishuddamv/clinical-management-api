package com.clinicos.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight response DTO for Patient lists
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSummaryResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String gender;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static PatientSummaryResponse fromEntity(Object[] row) {
        return PatientSummaryResponse.builder()
                .id((Long) row[0])
                .firstName((String) row[1])
                .lastName((String) row[2])
                .phone((String) row[3])
                .gender(((Enum<?>) row[4]).toString())
                .isActive((Boolean) row[5])
                .createdAt((LocalDateTime) row[6])
                .build();
    }
}

