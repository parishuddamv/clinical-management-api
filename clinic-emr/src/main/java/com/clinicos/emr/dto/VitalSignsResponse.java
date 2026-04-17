package com.clinicos.emr.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignsResponse {
    private Long id;
    private Long patientId;
    private Long visitId;
    private LocalDateTime recordedAt;
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private String bloodPressure;
    private Integer heartRate;
    private Integer respiratoryRate;
    private Double temperature;
    private String temperatureUnit;
    private Double oxygenSaturation;
    private Double weight;
    private String weightUnit;
    private Double height;
    private String heightUnit;
    private Double bmi;
    private Double bloodSugar;
    private String notes;
    private String recordedBy;
    private LocalDateTime createdAt;

    public String getBloodPressure() {
        if (bloodPressureSystolic != null && bloodPressureDiastolic != null) {
            return bloodPressureSystolic + "/" + bloodPressureDiastolic + " mmHg";
        }
        return null;
    }
}

