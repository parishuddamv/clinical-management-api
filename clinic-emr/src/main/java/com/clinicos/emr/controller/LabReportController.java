package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.LabReport.LabReportStatus;
import com.clinicos.emr.service.LabReportService;
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
@RequestMapping("/api/v1/emr/lab-reports")
@RequiredArgsConstructor
public class LabReportController {

    private final LabReportService labReportService;

    /**
     * Create a new lab report
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LabReportResponse>> createLabReport(
            @Valid @RequestBody CreateLabReportRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        LabReportResponse response = labReportService.createLabReport(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab report created successfully", response));
    }

    /**
     * Get lab report by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabReportResponse>> getLabReport(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        LabReportResponse response = labReportService.getLabReport(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update lab report (e.g., add results)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LabReportResponse>> updateLabReport(
            @PathVariable Long id,
            @RequestBody CreateLabReportRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        LabReportResponse response = labReportService.updateLabReport(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Lab report updated successfully", response));
    }

    /**
     * Get lab reports for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<LabReportResponse>>> getPatientLabReports(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<LabReportResponse> results = labReportService.getPatientLabReports(
                clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get all lab reports with optional status filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LabReportResponse>>> getAllLabReports(
            @RequestParam(required = false) LabReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<LabReportResponse> results = labReportService.getAllLabReports(
                clinicId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

