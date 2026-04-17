package com.clinicos.appointment.dto;

import com.clinicos.appointment.entity.Appointment.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAppointmentRequest {

    private LocalDateTime appointmentDateTime;

    private AppointmentStatus status;

    private String doctorName;

    private String notes;
}
