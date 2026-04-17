package com.clinicos.appointment.controller;

import com.clinicos.appointment.dto.AppointmentResponse;
import com.clinicos.appointment.dto.CreateAppointmentRequest;
import com.clinicos.appointment.dto.UpdateAppointmentRequest;
import com.clinicos.appointment.entity.Appointment.AppointmentStatus;
import com.clinicos.appointment.service.AppointmentService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * POST /api/v1/appointments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        AppointmentResponse response = appointmentService.create(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment created successfully", response));
    }

    /**
     * GET /api/v1/appointments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        AppointmentResponse response = appointmentService.getById(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/appointments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateAppointmentRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        AppointmentResponse response = appointmentService.update(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Appointment updated successfully", response));
    }

    /**
     * GET /api/v1/appointments?status=&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getAll(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<AppointmentResponse> results = appointmentService.getAll(clinicId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * GET /api/v1/appointments/today - Get today's appointments for the dashboard
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getTodayAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<AppointmentResponse> results = appointmentService.getTodayAppointments(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}
