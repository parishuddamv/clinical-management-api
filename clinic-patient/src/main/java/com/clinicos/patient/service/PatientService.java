package com.clinicos.patient.service;

import com.clinicos.patient.dto.*;
import com.clinicos.patient.entity.Patient;
import com.clinicos.patient.entity.PatientTag;
import com.clinicos.patient.exception.DuplicatePatientException;
import com.clinicos.patient.exception.PatientNotFoundException;
import com.clinicos.patient.repository.PatientRepository;
import com.clinicos.patient.repository.PatientTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PatientService provides business logic for patient management.
 * Handles registration, updates, search, and tag operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientTagRepository patientTagRepository;

    /**
     * Register a new patient for a clinic
     */
    @Transactional
    public PatientResponse registerPatient(String clinicId, RegisterPatientRequest request) {
        log.info("Registering new patient for clinic: {}", clinicId);

        // Check for duplicate phone per clinic
        patientRepository.findByClinicIdAndPhone(clinicId, request.getPhone())
                .ifPresent(p -> {
                    throw new DuplicatePatientException(request.getPhone());
                });

        // Create and save patient
        Patient patient = new Patient();
        patient.setClinicId(clinicId);
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setPhone(request.getPhone());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        patient.setIsActive(true);

        Patient savedPatient = patientRepository.save(patient);
        log.info("Patient registered successfully with ID: {}", savedPatient.getId());

        return PatientResponse.fromEntity(savedPatient, List.of());
    }

    /**
     * Get patient by ID (with clinic isolation)
     * Cached for 1 hour for performance
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "patients", key = "#clinicId + ':' + #patientId", unless = "#result == null")
    public PatientResponse getPatientById(String clinicId, Long patientId) {
        Patient patient = patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        List<String> tags = patientTagRepository.findByClinicIdAndPatientId(clinicId, patientId)
                .stream()
                .map(PatientTag::getTag)
                .collect(Collectors.toList());

        return PatientResponse.fromEntity(patient, tags);
    }

    /**
     * Update patient information (partial update)
     * Invalidates patient cache after update
     */
    @Transactional
    @CacheEvict(value = "patients", key = "#clinicId + ':' + #patientId")
    public PatientResponse updatePatient(String clinicId, Long patientId, UpdatePatientRequest request) {
        log.info("Updating patient: {} for clinic: {}", patientId, clinicId);

        Patient patient = patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        // Check for duplicate phone if phone is being updated
        if (request.getPhone() != null && !request.getPhone().equals(patient.getPhone())) {
            patientRepository.findByClinicIdAndPhone(clinicId, request.getPhone())
                    .ifPresent(p -> {
                        throw new DuplicatePatientException(request.getPhone());
                    });
            patient.setPhone(request.getPhone());
        }

        // Update fields if provided
        if (request.getFirstName() != null) patient.setFirstName(request.getFirstName());
        if (request.getLastName() != null) patient.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) patient.setGender(request.getGender());
        if (request.getBloodGroup() != null) patient.setBloodGroup(request.getBloodGroup());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        if (request.getEmergencyContactName() != null) patient.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getIsActive() != null) patient.setIsActive(request.getIsActive());

        Patient updatedPatient = patientRepository.save(patient);
        log.info("Patient updated successfully: {}", patientId);

        List<String> tags = patientTagRepository.findByClinicIdAndPatientId(clinicId, patientId)
                .stream()
                .map(PatientTag::getTag)
                .collect(Collectors.toList());

        return PatientResponse.fromEntity(updatedPatient, tags);
    }

    /**
     * Search patients by name or phone with pagination
     * Intentionally not cached: Redis JSON deserialization of PageImpl can fail
     * when reading back cached values across service versions/configurations.
     */
    @Transactional(readOnly = true)
    public Page<PatientSummaryResponse> searchPatients(String clinicId, String query, Pageable pageable) {
        log.info("Searching patients for clinic: {} with query: {}", clinicId, query);

        Page<Patient> patients = patientRepository.searchByName(clinicId, query, pageable);

        // If name search returns empty, try phone search
        if (patients.isEmpty()) {
            patients = patientRepository.searchByPhone(clinicId, query, pageable);
        }

        return patients.map(p -> PatientSummaryResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .gender(p.getGender().toString())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build());
    }

    /**
     * Soft delete patient by setting isActive to false
     * Invalidates patient cache after deletion
     */
    @Transactional
    @CacheEvict(value = {"patients", "patientSearch"}, allEntries = true)
    public void softDeletePatient(String clinicId, Long patientId) {
        log.info("Soft deleting patient: {} for clinic: {}", patientId, clinicId);

        Patient patient = patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        patient.setIsActive(false);
        patientRepository.save(patient);
        log.info("Patient soft deleted: {}", patientId);
    }

    /**
     * Add a tag to patient
     */
    @Transactional
    public void addTag(String clinicId, Long patientId, String tag) {
        log.info("Adding tag '{}' to patient: {} for clinic: {}", tag, patientId, clinicId);

        // Verify patient exists
        patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        // Check if tag already exists
        if (!patientTagRepository.existsByClinicIdAndPatientIdAndTag(clinicId, patientId, tag)) {
            PatientTag patientTag = new PatientTag();
            patientTag.setClinicId(clinicId);
            patientTag.setPatientId(patientId);
            patientTag.setTag(tag);
            patientTagRepository.save(patientTag);
            log.info("Tag added successfully");
        }
    }

    /**
     * Remove a tag from patient
     */
    @Transactional
    public void removeTag(String clinicId, Long patientId, String tag) {
        log.info("Removing tag '{}' from patient: {} for clinic: {}", tag, patientId, clinicId);

        patientTagRepository.findByClinicIdAndPatientIdAndTag(clinicId, patientId, tag)
                .ifPresent(patientTagRepository::delete);
    }

    /**
     * Get all tags for a patient
     */
    @Transactional(readOnly = true)
    public List<String> getPatientTags(String clinicId, Long patientId) {
        return patientTagRepository.findByClinicIdAndPatientId(clinicId, patientId)
                .stream()
                .map(PatientTag::getTag)
                .collect(Collectors.toList());
    }
}

