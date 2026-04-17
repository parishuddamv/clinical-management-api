package com.clinicos.emr.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Vital Signs entity for tracking patient vitals history.
 */
@Entity
@Table(name = "vital_signs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSigns extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private PatientVisit visit;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "blood_pressure_systolic")
    private Integer bloodPressureSystolic;

    @Column(name = "blood_pressure_diastolic")
    private Integer bloodPressureDiastolic;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "temperature_unit", length = 1)
    @Builder.Default
    private String temperatureUnit = "C";

    @Column(name = "oxygen_saturation")
    private Double oxygenSaturation;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "weight_unit", length = 2)
    @Builder.Default
    private String weightUnit = "kg";

    @Column(name = "height")
    private Double height;

    @Column(name = "height_unit", length = 2)
    @Builder.Default
    private String heightUnit = "cm";

    @Column(name = "bmi")
    private Double bmi;

    @Column(name = "blood_sugar")
    private Double bloodSugar;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    @PrePersist
    protected void onPrePersist() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
        calculateBmi();
    }

    @PreUpdate
    protected void onPreUpdate() {
        calculateBmi();
    }

    private void calculateBmi() {
        if (weight != null && height != null && height > 0) {
            double heightInMeters = height / 100.0;
            this.bmi = Math.round((weight / (heightInMeters * heightInMeters)) * 10.0) / 10.0;
        }
    }
}

