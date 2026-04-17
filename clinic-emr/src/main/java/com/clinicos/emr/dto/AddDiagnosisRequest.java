package com.clinicos.emr.dto;

import com.clinicos.emr.entity.Diagnosis.DiagnosisType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDiagnosisRequest {

    @NotNull(message = "Visit ID is required")
    private Long visitId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private String icdCode;

    @NotBlank(message = "Diagnosis name is required")
    private String diagnosisName;

    private DiagnosisType diagnosisType;

    private String severity;

    private LocalDate onsetDate;

    private LocalDate resolutionDate;

    private Boolean isChronic;

    private String notes;
}

