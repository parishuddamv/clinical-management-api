package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.PatientAllergy;
import com.clinicos.emr.entity.PatientAllergy.AllergenType;
import com.clinicos.emr.repository.PatientAllergyRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing patient allergies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllergyService {

    private final PatientAllergyRepository allergyRepository;

    /**
     * Create a new allergy record.
     */
    @Transactional
    public AllergyResponse createAllergy(String clinicId, CreateAllergyRequest request, String recordedBy) {
        log.info("Creating allergy record for patient: {} in clinic: {}", request.getPatientId(), clinicId);

        // Check if allergy already exists
        if (allergyRepository.existsByPatientAndAllergen(clinicId, request.getPatientId(), request.getAllergenName())) {
            throw new IllegalArgumentException("Allergy already recorded for this patient: " + request.getAllergenName());
        }

        PatientAllergy allergy = PatientAllergy.builder()
                .patientId(request.getPatientId())
                .allergenType(request.getAllergenType())
                .allergenName(request.getAllergenName())
                .reactionType(request.getReactionType())
                .severity(request.getSeverity())
                .onsetDate(request.getOnsetDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .notes(request.getNotes())
                .recordedBy(recordedBy)
                .build();

        allergy.setClinicId(clinicId);
        allergy = allergyRepository.save(allergy);

        log.info("Created allergy record with ID: {}", allergy.getId());
        return mapToResponse(allergy);
    }

    /**
     * Get allergy by ID.
     */
    @Transactional(readOnly = true)
    public AllergyResponse getAllergy(String clinicId, Long id) {
        PatientAllergy allergy = allergyRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found: " + id));
        return mapToResponse(allergy);
    }

    /**
     * Update allergy record.
     */
    @Transactional
    public AllergyResponse updateAllergy(String clinicId, Long id, CreateAllergyRequest request) {
        PatientAllergy allergy = allergyRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found: " + id));

        if (request.getAllergenType() != null) allergy.setAllergenType(request.getAllergenType());
        if (request.getAllergenName() != null) allergy.setAllergenName(request.getAllergenName());
        if (request.getReactionType() != null) allergy.setReactionType(request.getReactionType());
        if (request.getSeverity() != null) allergy.setSeverity(request.getSeverity());
        if (request.getOnsetDate() != null) allergy.setOnsetDate(request.getOnsetDate());
        if (request.getIsActive() != null) allergy.setIsActive(request.getIsActive());
        if (request.getNotes() != null) allergy.setNotes(request.getNotes());

        allergy = allergyRepository.save(allergy);
        return mapToResponse(allergy);
    }

    /**
     * Deactivate allergy (soft delete).
     */
    @Transactional
    public void deactivateAllergy(String clinicId, Long id) {
        PatientAllergy allergy = allergyRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found: " + id));
        allergy.setIsActive(false);
        allergyRepository.save(allergy);
        log.info("Deactivated allergy: {}", id);
    }

    /**
     * Delete allergy record permanently.
     */
    @Transactional
    public void deleteAllergy(String clinicId, Long id) {
        PatientAllergy allergy = allergyRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found: " + id));
        allergyRepository.delete(allergy);
        log.info("Deleted allergy: {}", id);
    }

    /**
     * Get all allergies for a patient (paginated).
     */
    @Transactional(readOnly = true)
    public Page<AllergyResponse> getPatientAllergies(String clinicId, Long patientId, Pageable pageable) {
        return allergyRepository.findByClinicIdAndPatientIdOrderByCreatedAtDesc(clinicId, patientId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get active allergies for a patient.
     */
    @Transactional(readOnly = true)
    public List<AllergyResponse> getActiveAllergies(String clinicId, Long patientId) {
        return allergyRepository.findByClinicIdAndPatientIdAndIsActiveTrueOrderByAllergenTypeAsc(clinicId, patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get allergies by type for a patient.
     */
    @Transactional(readOnly = true)
    public List<AllergyResponse> getAllergiesByType(String clinicId, Long patientId, AllergenType type) {
        return allergyRepository.findByPatientAndType(clinicId, patientId, type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get drug allergies for a patient (important for prescription safety).
     */
    @Transactional(readOnly = true)
    public List<AllergyResponse> getDrugAllergies(String clinicId, Long patientId) {
        return allergyRepository.findDrugAllergies(clinicId, patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AllergyResponse mapToResponse(PatientAllergy a) {
        return AllergyResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .allergenType(a.getAllergenType())
                .allergenName(a.getAllergenName())
                .reactionType(a.getReactionType())
                .severity(a.getSeverity())
                .onsetDate(a.getOnsetDate())
                .isActive(a.getIsActive())
                .notes(a.getNotes())
                .recordedBy(a.getRecordedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
