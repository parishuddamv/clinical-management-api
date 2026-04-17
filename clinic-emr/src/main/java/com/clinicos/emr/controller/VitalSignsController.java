package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.service.VitalSignsService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for managing vital signs records.
 * 
 * Endpoints:
 * - POST   /api/v1/emr/vitals              - Record vital signs
 * - GET    /api/v1/emr/vitals/{id}         - Get vital signs by ID
 * - PUT    /api/v1/emr/vitals/{id}         - Update vital signs
 * - DELETE /api/v1/emr/vitals/{id}         - Delete vital signs
 * - GET    /api/v1/emr/vitals/patient/{id} - Get patient vital signs history
 * - GET    /api/v1/emr/vitals/patient/{id}/latest - Get latest vitals
 * - GET    /api/v1/emr/vitals/visit/{id}   - Get vital signs for a visit
 * - GET    /api/v1/emr/vitals/patient/{id}/range - Get vitals in date range
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/emr/vitals")
@RequiredArgsConstructor
public class VitalSignsController {

    private final VitalSignsService vitalSignsService;

    /**
     * Record new vital signs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VitalSignsResponse>> recordVitalSigns(
            @Valid @RequestBody CreateVitalSignsRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        String recordedBy = authentication.getName();
        VitalSignsResponse response = vitalSignsService.createVitalSigns(clinicId, request, recordedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vital signs recorded successfully", response));
    }

    /**
     * Get vital signs by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VitalSignsResponse>> getVitalSigns(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VitalSignsResponse response = vitalSignsService.getVitalSigns(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update vital signs
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VitalSignsResponse>> updateVitalSigns(
            @PathVariable Long id,
            @RequestBody CreateVitalSignsRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VitalSignsResponse response = vitalSignsService.updateVitalSigns(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Vital signs updated successfully", response));
    }

    /**
     * Delete vital signs
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVitalSigns(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        vitalSignsService.deleteVitalSigns(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Vital signs deleted successfully", null));
    }

    /**
     * Get vital signs history for a patient (paginated)
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<VitalSignsResponse>>> getPatientVitalSigns(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<VitalSignsResponse> results = vitalSignsService.getPatientVitalSigns(
                clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get latest vital signs for a patient
     */
    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<ApiResponse<VitalSignsResponse>> getLatestVitalSigns(
            @PathVariable Long patientId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VitalSignsResponse response = vitalSignsService.getLatestVitalSigns(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get vital signs for a specific visit
     */
    @GetMapping("/visit/{visitId}")
    public ResponseEntity<ApiResponse<List<VitalSignsResponse>>> getVisitVitalSigns(
            @PathVariable Long visitId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<VitalSignsResponse> results = vitalSignsService.getVisitVitalSigns(clinicId, visitId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get vital signs in a date range (for trending/graphing)
     */
    @GetMapping("/patient/{patientId}/range")
    public ResponseEntity<ApiResponse<List<VitalSignsResponse>>> getVitalSignsInRange(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<VitalSignsResponse> results = vitalSignsService.getVitalSignsInRange(
                clinicId, patientId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}
