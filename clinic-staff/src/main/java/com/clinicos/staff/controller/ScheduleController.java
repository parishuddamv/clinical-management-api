package com.clinicos.staff.controller;

import com.clinicos.staff.dto.DoctorScheduleDTO;
import com.clinicos.staff.entity.ScheduleOverride;
import com.clinicos.staff.service.ScheduleService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorScheduleDTO>> createSchedule(
            @Valid @RequestBody DoctorScheduleDTO dto,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        DoctorScheduleDTO response = scheduleService.createSchedule(clinicId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Schedule created successfully", response));
    }

    @PostMapping("/staff/{staffId}/weekly")
    public ResponseEntity<ApiResponse<List<DoctorScheduleDTO>>> setWeeklySchedule(
            @PathVariable Long staffId,
            @Valid @RequestBody List<DoctorScheduleDTO> schedules,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<DoctorScheduleDTO> response = scheduleService.setWeeklySchedule(clinicId, staffId, schedules);
        return ResponseEntity.ok(ApiResponse.success("Weekly schedule updated", response));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<DoctorScheduleDTO>>> getStaffSchedule(
            @PathVariable Long staffId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<DoctorScheduleDTO> schedules = scheduleService.getStaffSchedule(clinicId, staffId);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    @GetMapping("/staff/{staffId}/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorAvailability(
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Map<String, Object> availability = scheduleService.getDoctorAvailability(clinicId, staffId, date);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    @PostMapping("/staff/{staffId}/override")
    public ResponseEntity<ApiResponse<Void>> addScheduleOverride(
            @PathVariable Long staffId,
            @RequestParam ScheduleOverride.OverrideType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        scheduleService.addScheduleOverride(clinicId, staffId, type, date, startTime, endTime, reason);
        return ResponseEntity.ok(ApiResponse.success("Schedule override added", null));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

