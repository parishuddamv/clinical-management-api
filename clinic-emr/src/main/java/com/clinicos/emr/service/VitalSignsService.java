package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.*;
import com.clinicos.emr.repository.*;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing vital signs records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VitalSignsService {

    private final VitalSignsRepository vitalSignsRepository;
    private final PatientVisitRepository visitRepository;

    /**
     * Create a new vital signs record.
     */
    @Transactional
    public VitalSignsResponse createVitalSigns(String clinicId, CreateVitalSignsRequest request, String recordedBy) {
        log.info("Recording vital signs for patient: {} in clinic: {}", request.getPatientId(), clinicId);

        VitalSigns vitals = VitalSigns.builder()
                .patientId(request.getPatientId())
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now())
                .bloodPressureSystolic(request.getBloodPressureSystolic())
                .bloodPressureDiastolic(request.getBloodPressureDiastolic())
                .heartRate(request.getHeartRate())
                .respiratoryRate(request.getRespiratoryRate())
                .temperature(request.getTemperature())
                .temperatureUnit(request.getTemperatureUnit() != null ? request.getTemperatureUnit() : "C")
                .oxygenSaturation(request.getOxygenSaturation())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit() != null ? request.getWeightUnit() : "kg")
                .height(request.getHeight())
                .heightUnit(request.getHeightUnit() != null ? request.getHeightUnit() : "cm")
                .bloodSugar(request.getBloodSugar())
                .notes(request.getNotes())
                .recordedBy(recordedBy)
                .build();

        vitals.setClinicId(clinicId);

        // Link to visit if provided
        if (request.getVisitId() != null) {
            visitRepository.findByIdAndClinicId(request.getVisitId(), clinicId)
                    .ifPresent(vitals::setVisit);
        }

        vitals = vitalSignsRepository.save(vitals);
        log.info("Created vital signs record with ID: {}", vitals.getId());

        return mapToResponse(vitals);
    }

    /**
     * Get vital signs by ID.
     */
    @Transactional(readOnly = true)
    public VitalSignsResponse getVitalSigns(String clinicId, Long id) {
        VitalSigns vitals = vitalSignsRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Vital signs not found: " + id));
        return mapToResponse(vitals);
    }

    /**
     * Update vital signs record.
     */
    @Transactional
    public VitalSignsResponse updateVitalSigns(String clinicId, Long id, CreateVitalSignsRequest request) {
        VitalSigns vitals = vitalSignsRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Vital signs not found: " + id));

        if (request.getBloodPressureSystolic() != null) vitals.setBloodPressureSystolic(request.getBloodPressureSystolic());
        if (request.getBloodPressureDiastolic() != null) vitals.setBloodPressureDiastolic(request.getBloodPressureDiastolic());
        if (request.getHeartRate() != null) vitals.setHeartRate(request.getHeartRate());
        if (request.getRespiratoryRate() != null) vitals.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getTemperature() != null) vitals.setTemperature(request.getTemperature());
        if (request.getTemperatureUnit() != null) vitals.setTemperatureUnit(request.getTemperatureUnit());
        if (request.getOxygenSaturation() != null) vitals.setOxygenSaturation(request.getOxygenSaturation());
        if (request.getWeight() != null) vitals.setWeight(request.getWeight());
        if (request.getWeightUnit() != null) vitals.setWeightUnit(request.getWeightUnit());
        if (request.getHeight() != null) vitals.setHeight(request.getHeight());
        if (request.getHeightUnit() != null) vitals.setHeightUnit(request.getHeightUnit());
        if (request.getBloodSugar() != null) vitals.setBloodSugar(request.getBloodSugar());
        if (request.getNotes() != null) vitals.setNotes(request.getNotes());

        vitals = vitalSignsRepository.save(vitals);
        return mapToResponse(vitals);
    }

    /**
     * Delete vital signs record.
     */
    @Transactional
    public void deleteVitalSigns(String clinicId, Long id) {
        VitalSigns vitals = vitalSignsRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Vital signs not found: " + id));
        vitalSignsRepository.delete(vitals);
        log.info("Deleted vital signs record: {}", id);
    }

    /**
     * Get vital signs history for a patient.
     */
    @Transactional(readOnly = true)
    public Page<VitalSignsResponse> getPatientVitalSigns(String clinicId, Long patientId, Pageable pageable) {
        return vitalSignsRepository.findByClinicIdAndPatientIdOrderByRecordedAtDesc(clinicId, patientId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get vital signs for a specific visit.
     */
    @Transactional(readOnly = true)
    public List<VitalSignsResponse> getVisitVitalSigns(String clinicId, Long visitId) {
        // Verify visit belongs to clinic
        visitRepository.findByIdAndClinicId(visitId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));

        return vitalSignsRepository.findByVisitIdOrderByRecordedAtDesc(visitId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get latest vital signs for a patient.
     */
    @Transactional(readOnly = true)
    public VitalSignsResponse getLatestVitalSigns(String clinicId, Long patientId) {
        return vitalSignsRepository.findLatestByPatient(clinicId, patientId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No vital signs found for patient: " + patientId));
    }

    /**
     * Get vital signs within a date range (for trending/graphing).
     */
    @Transactional(readOnly = true)
    public List<VitalSignsResponse> getVitalSignsInRange(String clinicId, Long patientId, 
                                                          LocalDateTime startDate, LocalDateTime endDate) {
        return vitalSignsRepository.findByPatientAndDateRange(clinicId, patientId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VitalSignsResponse mapToResponse(VitalSigns v) {
        return VitalSignsResponse.builder()
                .id(v.getId())
                .patientId(v.getPatientId())
                .visitId(v.getVisit() != null ? v.getVisit().getId() : null)
                .recordedAt(v.getRecordedAt())
                .bloodPressureSystolic(v.getBloodPressureSystolic())
                .bloodPressureDiastolic(v.getBloodPressureDiastolic())
                .heartRate(v.getHeartRate())
                .respiratoryRate(v.getRespiratoryRate())
                .temperature(v.getTemperature())
                .temperatureUnit(v.getTemperatureUnit())
                .oxygenSaturation(v.getOxygenSaturation())
                .weight(v.getWeight())
                .weightUnit(v.getWeightUnit())
                .height(v.getHeight())
                .heightUnit(v.getHeightUnit())
                .bmi(v.getBmi())
                .bloodSugar(v.getBloodSugar())
                .notes(v.getNotes())
                .recordedBy(v.getRecordedBy())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
