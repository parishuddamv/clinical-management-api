package com.clinicos.patient.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * PatientTag entity for tagging patients with medical conditions or characteristics.
 * Many-to-many relationship with Patient through join table.
 */
@Entity
@Table(name = "patient_tags", indexes = {
    @Index(name = "idx_patients", columnList = "clinic_id, patient_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientTag extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String tag;

    /**
     * Tag examples: "diabetic", "hypertensive", "regular", "cardiac", "asthmatic"
     */
}

