package com.clinicos.appointment.dto;

import com.clinicos.appointment.entity.Appointment;
import com.clinicos.appointment.entity.Appointment.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {

    private Long id;
    private Long patientId;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String doctorName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AppointmentResponse from(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .status(appointment.getStatus())
                .doctorName(appointment.getDoctorName())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
