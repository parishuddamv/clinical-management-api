package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.PatientAllergy.AllergenType;
import com.clinicos.emr.service.AllergyService;
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
 * Controller for managing patient allergies.
 * 
 * Endpoints:
 * - POST   /api/v1/emr/allergies              - Create allergy
 * - GET    /api/v1/emr/allergies/{id}         - Get allergy by ID
 * - PUT    /api/v1/emr/allergies/{id}         - Update allergy
 * - DELETE /api/v1/emr/allergies/{id}         - Delete allergy
 * - PUT    /api/v1/emr/allergies/{id}/deactivate - Deactivate allergy
 * - GET    /api/v1/emr/allergies/patient/{id} - Get all allergies for patient
 * - GET    /api/v1/emr/allergies/patient/{id}/active - Get active allergies
 * - GET    /api/v1/emr/allergies/patient/{id}/drugs - Get drug allergies
 * - GET    /api/v1/emr/allergies/patient/{id}/type/{type} - Get allergies by type
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/emr/allergies")
@RequiredArgsConstructor
public class AllergyController {

    private final AllergyService allergyService;

    /**
     * Create a new allergy record
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AllergyResponse>> createAllergy(
            @Valid @RequestBody CreateAllergyRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        String recordedBy = authentication.getName();
        AllergyResponse response = allergyService.createAllergy(clinicId, request, recordedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Allergy recorded successfully", response));
    }

    /**
     * Get allergy by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AllergyResponse>> getAllergy(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        AllergyResponse response = allergyService.getAllergy(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update allergy
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AllergyResponse>> updateAllergy(
            @PathVariable Long id,
            @RequestBody CreateAllergyRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        AllergyResponse response = allergyService.updateAllergy(clinicId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Allergy updated successfully", response));
    }

    /**
     * Deactivate allergy (soft delete)
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAllergy(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        allergyService.deactivateAllergy(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Allergy deactivated successfully", null));
    }

    /**
     * Delete allergy (permanent)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAllergy(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        allergyService.deleteAllergy(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("Allergy deleted successfully", null));
    }

    /**
     * Get all allergies for a patient (paginated)
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<AllergyResponse>>> getPatientAllergies(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<AllergyResponse> results = allergyService.getPatientAllergies(
                clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get active allergies for a patient
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<ApiResponse<List<AllergyResponse>>> getActiveAllergies(
            @PathVariable Long patientId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<AllergyResponse> results = allergyService.getActiveAllergies(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get drug allergies for a patient (important for prescription safety checks)
     */
    @GetMapping("/patient/{patientId}/drugs")
    public ResponseEntity<ApiResponse<List<AllergyResponse>>> getDrugAllergies(
            @PathVariable Long patientId,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<AllergyResponse> results = allergyService.getDrugAllergies(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get allergies by type for a patient
     */
    @GetMapping("/patient/{patientId}/type/{type}")
    public ResponseEntity<ApiResponse<List<AllergyResponse>>> getAllergiesByType(
            @PathVariable Long patientId,
            @PathVariable AllergenType type,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        List<AllergyResponse> results = allergyService.getAllergiesByType(clinicId, patientId, type);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}
