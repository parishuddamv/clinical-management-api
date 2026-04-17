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

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

