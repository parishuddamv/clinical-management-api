package com.clinicos.emr.dto;

import com.clinicos.emr.entity.PatientAllergy.AllergenType;
import com.clinicos.emr.entity.PatientAllergy.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating patient allergies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAllergyRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Allergen type is required")
    private AllergenType allergenType;

    @NotBlank(message = "Allergen name is required")
    private String allergenName;

    private String reactionType;

    private AllergySeverity severity;

    private LocalDate onsetDate;

    private Boolean isActive;

    private String notes;
}
