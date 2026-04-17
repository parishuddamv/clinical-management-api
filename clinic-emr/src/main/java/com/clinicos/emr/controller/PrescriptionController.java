package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.Prescription.PrescriptionStatus;
import com.clinicos.emr.service.PrescriptionService;
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
@RequestMapping("/api/v1/emr/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * Create a new prescription
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        PrescriptionResponse response = prescriptionService.createPrescription(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prescription created successfully", response));
    }

    /**
     * Get prescription by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescription(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        PrescriptionResponse response = prescriptionService.getPrescription(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Finalize a draft prescription
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> finalizePrescription(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        PrescriptionResponse response = prescriptionService.finalizePrescription(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Prescription finalized", response));
    }

    /**
     * Deliver prescription (Print/Email/WhatsApp)
     */
    @PostMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> deliverPrescription(
            @PathVariable Long id,
            @Valid @RequestBody DeliverPrescriptionRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        PrescriptionResponse response = prescriptionService.deliverPrescription(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Prescription delivered via " + request.getDeliveryMethod(), response));
    }

    /**
     * Get prescriptions for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<PrescriptionResponse>>> getPatientPrescriptions(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<PrescriptionResponse> results = prescriptionService.getPatientPrescriptions(clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get all prescriptions with optional status filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PrescriptionResponse>>> getAllPrescriptions(
            @RequestParam(required = false) PrescriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<PrescriptionResponse> results = prescriptionService.getAllPrescriptions(clinicId, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Search drugs for auto-suggestions
     */
    @GetMapping("/drugs/search")
    public ResponseEntity<ApiResponse<List<DrugResponse>>> searchDrugs(
            @RequestParam String q,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<DrugResponse> results = prescriptionService.searchDrugs(clinicId, q);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

