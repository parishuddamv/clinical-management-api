package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.service.VisitService;
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
@RequestMapping("/api/v1/emr/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    /**
     * Create a new patient visit/consultation
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VisitResponse>> createVisit(
            @Valid @RequestBody CreateVisitRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VisitResponse response = visitService.createVisit(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Visit created successfully", response));
    }

    /**
     * Get visit by ID with full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VisitResponse>> getVisit(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VisitResponse response = visitService.getVisit(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update visit
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VisitResponse>> updateVisit(
            @PathVariable Long id,
            @RequestBody CreateVisitRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VisitResponse response = visitService.updateVisit(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Visit updated successfully", response));
    }

    /**
     * Complete a visit
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<VisitResponse>> completeVisit(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        VisitResponse response = visitService.completeVisit(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Visit completed", response));
    }

    /**
     * Get all visits for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<VisitResponse>>> getPatientVisits(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<VisitResponse> results = visitService.getVisitsByPatient(clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get all visits (for dashboard)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VisitResponse>>> getAllVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<VisitResponse> results = visitService.getAllVisits(clinicId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

