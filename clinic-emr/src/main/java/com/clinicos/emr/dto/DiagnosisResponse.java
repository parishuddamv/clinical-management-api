package com.clinicos.emr.dto;

import com.clinicos.emr.entity.Diagnosis.DiagnosisType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponse {
    private Long id;
    private Long visitId;
    private Long patientId;
    private String icdCode;
    private String diagnosisName;
    private DiagnosisType diagnosisType;
    private String severity;
    private LocalDate onsetDate;
    private LocalDate resolutionDate;
    private Boolean isChronic;
    private String notes;
    private LocalDateTime createdAt;
}

