package com.clinicos.appointment.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Appointment entity for clinic appointment scheduling
 */
@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_patients", columnList = "clinic_id, patient_id"),
    @Index(name = "idx_clinic_datetime", columnList = "clinic_id, appointment_datetime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Column(name = "appointment_datetime", nullable = false)
    private LocalDateTime appointmentDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "doctor_name")
    private String doctorName;

    public enum AppointmentStatus {
        SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
    }
}

