package com.clinicos.emr.dto;

import com.clinicos.emr.entity.PatientAllergy.AllergenType;
import com.clinicos.emr.entity.PatientAllergy.AllergySeverity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for patient allergy records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllergyResponse {
    private Long id;
    private Long patientId;
    private AllergenType allergenType;
    private String allergenName;
    private String reactionType;
    private AllergySeverity severity;
    private LocalDate onsetDate;
    private Boolean isActive;
    private String notes;
    private String recordedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
