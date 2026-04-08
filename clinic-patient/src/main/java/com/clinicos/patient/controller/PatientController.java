package com.clinicos.patient.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.patient.dto.*;
import com.clinicos.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * PatientController handles all patient-related endpoints.
 * All endpoints are clinic-scoped using clinic_id from JWT.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * Register a new patient
     * POST /api/v1/patients
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> registerPatient(
            @Valid @RequestBody RegisterPatientRequest request,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        PatientResponse response = patientService.registerPatient(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient registered successfully", response));
    }

    /**
     * Get patient by ID
     * GET /api/v1/patients/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        PatientResponse response = patientService.getPatientById(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update patient
     * PUT /api/v1/patients/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientRequest request,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        PatientResponse response = patientService.updatePatient(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", response));
    }

    /**
     * Soft delete patient (mark as inactive)
     * DELETE /api/v1/patients/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        patientService.softDeletePatient(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }

    /**
     * Search patients
     * GET /api/v1/patients/search?q=&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PatientSummaryResponse>>> searchPatients(
            @RequestParam String q,
            Pageable pageable,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        Page<PatientSummaryResponse> results = patientService.searchPatients(clinicId, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Add tag to patient
     * POST /api/v1/patients/{id}/tags
     */
    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<Void>> addTag(
            @PathVariable Long id,
            @RequestBody TagRequest tagRequest,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        patientService.addTag(clinicId, id, tagRequest.getTag());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tag added successfully", null));
    }

    /**
     * Remove tag from patient
     * DELETE /api/v1/patients/{id}/tags/{tag}
     */
    @DeleteMapping("/{id}/tags/{tag}")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @PathVariable Long id,
            @PathVariable String tag,
            Authentication authentication) {
        String clinicId = getClinicIdFromToken(authentication);
        patientService.removeTag(clinicId, id, tag);
        return ResponseEntity.ok(ApiResponse.success("Tag removed successfully", null));
    }

    /**
     * Extract clinic ID from JWT authentication (from Security context)
     */
    private String getClinicIdFromToken(Authentication authentication) {
        // In a real scenario, clinic_id should be extracted from the JWT token
        // For now, this is a placeholder - the actual implementation would
        // extract it from SecurityContext or a custom principal
        return (String) authentication.getDetails();
    }

    /**
     * Simple DTO for tag requests
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TagRequest {
        private String tag;
    }
}

