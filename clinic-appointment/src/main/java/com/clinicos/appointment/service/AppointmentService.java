package com.clinicos.appointment.service;

import com.clinicos.appointment.dto.AppointmentResponse;
import com.clinicos.appointment.dto.CreateAppointmentRequest;
import com.clinicos.appointment.dto.UpdateAppointmentRequest;
import com.clinicos.appointment.entity.Appointment;
import com.clinicos.appointment.entity.Appointment.AppointmentStatus;
import com.clinicos.appointment.repository.AppointmentRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public AppointmentResponse create(String clinicId, CreateAppointmentRequest request) {
        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .appointmentDateTime(request.getAppointmentDateTime())
                .doctorName(request.getDoctorName())
                .notes(request.getNotes())
                .status(AppointmentStatus.SCHEDULED)
                .build();
        appointment.setClinicId(clinicId);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(String clinicId, Long id) {
        Appointment appointment = findByIdAndClinic(clinicId, id);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse update(String clinicId, Long id, UpdateAppointmentRequest request) {
        Appointment appointment = findByIdAndClinic(clinicId, id);

        if (request.getAppointmentDateTime() != null) {
            appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        }
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }
        if (request.getDoctorName() != null) {
            appointment.setDoctorName(request.getDoctorName());
        }
        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAll(String clinicId, AppointmentStatus status, Pageable pageable) {
        Page<Appointment> page = (status != null)
                ? appointmentRepository.findByClinicIdAndStatus(clinicId, status, pageable)
                : appointmentRepository.findByClinicId(clinicId, pageable);
        return page.map(AppointmentResponse::from);
    }

    /**
     * Get today's appointments for the clinic
     */
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getTodayAppointments(String clinicId, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        log.info("Fetching today's appointments for clinic: {} from {} to {}", clinicId, startOfDay, endOfDay);

        Page<Appointment> page = appointmentRepository.findByClinicIdAndDateRange(
                clinicId, startOfDay, endOfDay, pageable);
        return page.map(AppointmentResponse::from);
    }

    private Appointment findByIdAndClinic(String clinicId, Long id) {
        return appointmentRepository.findById(id)
                .filter(a -> clinicId.equals(a.getClinicId()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }
}
