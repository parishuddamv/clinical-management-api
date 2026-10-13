package com.clinicos.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAppointmentRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Appointment date/time is required")
    private LocalDateTime appointmentDateTime;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    private String notes;
}

