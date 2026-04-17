package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.*;
import com.clinicos.emr.entity.PatientVisit.VisitStatus;
import com.clinicos.emr.repository.*;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final PatientVisitRepository visitRepository;
    private final DiagnosisRepository diagnosisRepository;

    @Transactional
    public VisitResponse createVisit(String clinicId, CreateVisitRequest request) {
        log.info("Creating visit for patient: {} in clinic: {}", request.getPatientId(), clinicId);

        PatientVisit visit = PatientVisit.builder()
                .patientId(request.getPatientId())
                .appointmentId(request.getAppointmentId())
                .doctorId(request.getDoctorId())
                .doctorName(request.getDoctorName())
                .visitDateTime(request.getVisitDateTime() != null ? request.getVisitDateTime() : LocalDateTime.now())
                .visitType(request.getVisitType() != null ? request.getVisitType() : PatientVisit.VisitType.CONSULTATION)
                .chiefComplaint(request.getChiefComplaint())
                .presentIllness(request.getPresentIllness())
                .pastMedicalHistory(request.getPastMedicalHistory())
                .familyHistory(request.getFamilyHistory())
                .socialHistory(request.getSocialHistory())
                .allergies(request.getAllergies())
                .vitalSigns(request.getVitalSigns())
                .physicalExamination(request.getPhysicalExamination())
                .clinicalNotes(request.getClinicalNotes())
                .treatmentPlan(request.getTreatmentPlan())
                .followUpInstructions(request.getFollowUpInstructions())
                .status(VisitStatus.IN_PROGRESS)
                .build();

        visit.setClinicId(clinicId);
        visit = visitRepository.save(visit);

        log.info("Created visit with ID: {}", visit.getId());
        return mapToResponse(visit);
    }

    @Transactional(readOnly = true)
    public VisitResponse getVisit(String clinicId, Long visitId) {
        PatientVisit visit = visitRepository.findByIdWithDetailsAndClinicId(visitId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));
        return mapToResponse(visit);
    }

    @Transactional
    public VisitResponse updateVisit(String clinicId, Long visitId, CreateVisitRequest request) {
        PatientVisit visit = visitRepository.findByIdAndClinicId(visitId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));

        if (request.getChiefComplaint() != null) visit.setChiefComplaint(request.getChiefComplaint());
        if (request.getPresentIllness() != null) visit.setPresentIllness(request.getPresentIllness());
        if (request.getPastMedicalHistory() != null) visit.setPastMedicalHistory(request.getPastMedicalHistory());
        if (request.getFamilyHistory() != null) visit.setFamilyHistory(request.getFamilyHistory());
        if (request.getSocialHistory() != null) visit.setSocialHistory(request.getSocialHistory());
        if (request.getAllergies() != null) visit.setAllergies(request.getAllergies());
        if (request.getVitalSigns() != null) visit.setVitalSigns(request.getVitalSigns());
        if (request.getPhysicalExamination() != null) visit.setPhysicalExamination(request.getPhysicalExamination());
        if (request.getClinicalNotes() != null) visit.setClinicalNotes(request.getClinicalNotes());
        if (request.getTreatmentPlan() != null) visit.setTreatmentPlan(request.getTreatmentPlan());
        if (request.getFollowUpInstructions() != null) visit.setFollowUpInstructions(request.getFollowUpInstructions());

        visit = visitRepository.save(visit);
        return mapToResponse(visit);
    }

    @Transactional
    public VisitResponse completeVisit(String clinicId, Long visitId) {
        PatientVisit visit = visitRepository.findByIdAndClinicId(visitId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));
        visit.setStatus(VisitStatus.COMPLETED);
        visit = visitRepository.save(visit);
        return mapToResponse(visit);
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> getVisitsByPatient(String clinicId, Long patientId, Pageable pageable) {
        return visitRepository.findByClinicIdAndPatientIdOrderByVisitDateTimeDesc(clinicId, patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> getAllVisits(String clinicId, Pageable pageable) {
        return visitRepository.findByClinicIdOrderByVisitDateTimeDesc(clinicId, pageable)
                .map(this::mapToResponse);
    }

    private VisitResponse mapToResponse(PatientVisit visit) {
        return VisitResponse.builder()
                .id(visit.getId())
                .clinicId(visit.getClinicId())
                .patientId(visit.getPatientId())
                .appointmentId(visit.getAppointmentId())
                .doctorId(visit.getDoctorId())
                .doctorName(visit.getDoctorName())
                .visitDateTime(visit.getVisitDateTime())
                .visitType(visit.getVisitType())
                .chiefComplaint(visit.getChiefComplaint())
                .presentIllness(visit.getPresentIllness())
                .pastMedicalHistory(visit.getPastMedicalHistory())
                .familyHistory(visit.getFamilyHistory())
                .socialHistory(visit.getSocialHistory())
                .allergies(visit.getAllergies())
                .vitalSigns(visit.getVitalSigns())
                .physicalExamination(visit.getPhysicalExamination())
                .clinicalNotes(visit.getClinicalNotes())
                .treatmentPlan(visit.getTreatmentPlan())
                .followUpInstructions(visit.getFollowUpInstructions())
                .status(visit.getStatus())
                .diagnoses(visit.getDiagnoses() != null ? 
                    visit.getDiagnoses().stream().map(this::mapDiagnosisToResponse).collect(Collectors.toList()) : null)
                .createdAt(visit.getCreatedAt())
                .updatedAt(visit.getUpdatedAt())
                .build();
    }

    private DiagnosisResponse mapDiagnosisToResponse(Diagnosis diagnosis) {
        return DiagnosisResponse.builder()
                .id(diagnosis.getId())
                .visitId(diagnosis.getVisit().getId())
                .patientId(diagnosis.getPatientId())
                .icdCode(diagnosis.getIcdCode())
                .diagnosisName(diagnosis.getDiagnosisName())
                .diagnosisType(diagnosis.getDiagnosisType())
                .severity(diagnosis.getSeverity())
                .onsetDate(diagnosis.getOnsetDate())
                .resolutionDate(diagnosis.getResolutionDate())
                .isChronic(diagnosis.getIsChronic())
                .notes(diagnosis.getNotes())
                .createdAt(diagnosis.getCreatedAt())
                .build();
    }
}

