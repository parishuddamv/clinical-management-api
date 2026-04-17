package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.service.PatientHistoryService;
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

import java.util.List;

/**
 * Controller for patient history and diagnosis management.
 * 
 * Patient History Endpoints:
 * - GET /api/v1/emr/patients/{patientId}/history - Complete patient history timeline
 * 
 * Diagnosis Endpoints:
 * - POST   /api/v1/emr/diagnoses              - Add diagnosis to visit
 * - GET    /api/v1/emr/diagnoses/{id}         - Get diagnosis by ID
 * - PUT    /api/v1/emr/diagnoses/{id}         - Update diagnosis
 * - DELETE /api/v1/emr/diagnoses/{id}         - Delete diagnosis
 * - GET    /api/v1/emr/visits/{visitId}/diagnoses - Get diagnoses for visit
 * - GET    /api/v1/emr/patients/{patientId}/diagnoses - Get patient diagnoses
 * - GET    /api/v1/emr/patients/{patientId}/chronic - Get chronic conditions
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/emr")
@RequiredArgsConstructor
public class PatientHistoryController {

    private final PatientHistoryService patientHistoryService;

    /**
     * Get complete patient history/timeline
     */
    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<ApiResponse<PatientHistoryResponse>> getPatientHistory(
            @PathVariable Long patientId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        PatientHistoryResponse response = patientHistoryService.getPatientHistory(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Add diagnosis to a visit
     */
    @PostMapping("/diagnoses")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> addDiagnosis(
            @Valid @RequestBody AddDiagnosisRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        DiagnosisResponse response = patientHistoryService.addDiagnosis(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Diagnosis added successfully", response));
    }

    /**
     * Get diagnosis by ID
     */
    @GetMapping("/diagnoses/{id}")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> getDiagnosis(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        DiagnosisResponse response = patientHistoryService.getDiagnosis(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update diagnosis
     */
    @PutMapping("/diagnoses/{id}")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> updateDiagnosis(
            @PathVariable Long id,
            @RequestBody AddDiagnosisRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        DiagnosisResponse response = patientHistoryService.updateDiagnosis(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Diagnosis updated successfully", response));
    }

    /**
     * Delete diagnosis
     */
    @DeleteMapping("/diagnoses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDiagnosis(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        patientHistoryService.deleteDiagnosis(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Diagnosis deleted successfully", null));
    }

    /**
     * Get diagnoses for a visit
     */
    @GetMapping("/visits/{visitId}/diagnoses")
    public ResponseEntity<ApiResponse<List<DiagnosisResponse>>> getVisitDiagnoses(
            @PathVariable Long visitId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<DiagnosisResponse> results = patientHistoryService.getVisitDiagnoses(clinicId, visitId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get all diagnoses for a patient
     */
    @GetMapping("/patients/{patientId}/diagnoses")
    public ResponseEntity<ApiResponse<Page<DiagnosisResponse>>> getPatientDiagnoses(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<DiagnosisResponse> results = patientHistoryService.getPatientDiagnoses(
                clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get chronic conditions for a patient
     */
    @GetMapping("/patients/{patientId}/chronic")
    public ResponseEntity<ApiResponse<List<DiagnosisResponse>>> getChronicConditions(
            @PathVariable Long patientId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<DiagnosisResponse> results = patientHistoryService.getChronicConditions(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

