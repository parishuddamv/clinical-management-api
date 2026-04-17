package com.clinicos.emr.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Diagnosis entity for recording patient diagnoses.
 */
@Entity
@Table(name = "diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    private PatientVisit visit;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "icd_code", length = 20)
    private String icdCode;

    @Column(name = "diagnosis_name", nullable = false, length = 500)
    private String diagnosisName;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", nullable = false)
    @Builder.Default
    private DiagnosisType diagnosisType = DiagnosisType.PRIMARY;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolution_date")
    private LocalDate resolutionDate;

    @Column(name = "is_chronic")
    @Builder.Default
    private Boolean isChronic = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum DiagnosisType {
        PRIMARY,
        SECONDARY,
        DIFFERENTIAL,
        PROVISIONAL,
        FINAL
    }
}

