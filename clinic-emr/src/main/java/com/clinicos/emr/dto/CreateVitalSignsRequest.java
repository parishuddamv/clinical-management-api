package com.clinicos.emr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request DTO for creating or updating vital signs records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVitalSignsRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long visitId;

    private LocalDateTime recordedAt;

    // Blood Pressure
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;

    // Heart & Respiratory
    private Integer heartRate;
    private Integer respiratoryRate;

    // Temperature
    private Double temperature;
    private String temperatureUnit; // C or F

    // Oxygen Saturation
    private Double oxygenSaturation;

    // Weight & Height
    private Double weight;
    private String weightUnit; // kg or lb
    private Double height;
    private String heightUnit; // cm or in

    // Blood Sugar
    private Double bloodSugar;

    // Notes
    private String notes;
}
