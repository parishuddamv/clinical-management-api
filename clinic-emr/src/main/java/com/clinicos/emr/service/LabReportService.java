package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.*;
import com.clinicos.emr.entity.LabReport.LabReportStatus;
import com.clinicos.emr.repository.*;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabReportService {

    private final LabReportRepository labReportRepository;
    private final PatientVisitRepository visitRepository;

    @Transactional
    public LabReportResponse createLabReport(String clinicId, CreateLabReportRequest request) {
        log.info("Creating lab report for patient: {} in clinic: {}", request.getPatientId(), clinicId);

        LabReport labReport = LabReport.builder()
                .patientId(request.getPatientId())
                .reportNumber(request.getReportNumber())
                .testCategory(request.getTestCategory())
                .testName(request.getTestName())
                .testDate(request.getTestDate())
                .resultDate(request.getResultDate())
                .labName(request.getLabName())
                .orderingDoctor(request.getOrderingDoctor())
                .resultSummary(request.getResultSummary())
                .resultValues(request.getResultValues())
                .interpretation(request.getInterpretation())
                .referenceRange(request.getReferenceRange())
                .isAbnormal(request.getIsAbnormal() != null ? request.getIsAbnormal() : false)
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : LabReportStatus.PENDING)
                .build();

        labReport.setClinicId(clinicId);

        if (request.getVisitId() != null) {
            visitRepository.findByIdAndClinicId(request.getVisitId(), clinicId)
                    .ifPresent(labReport::setVisit);
        }

        labReport = labReportRepository.save(labReport);
        log.info("Created lab report with ID: {}", labReport.getId());

        return mapToResponse(labReport);
    }

    @Transactional(readOnly = true)
    public LabReportResponse getLabReport(String clinicId, Long id) {
        LabReport labReport = labReportRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report not found: " + id));
        return mapToResponse(labReport);
    }

    @Transactional
    public LabReportResponse updateLabReport(String clinicId, Long id, CreateLabReportRequest request) {
        LabReport labReport = labReportRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report not found: " + id));

        if (request.getResultSummary() != null) labReport.setResultSummary(request.getResultSummary());
        if (request.getResultValues() != null) labReport.setResultValues(request.getResultValues());
        if (request.getInterpretation() != null) labReport.setInterpretation(request.getInterpretation());
        if (request.getIsAbnormal() != null) labReport.setIsAbnormal(request.getIsAbnormal());
        if (request.getNotes() != null) labReport.setNotes(request.getNotes());
        if (request.getStatus() != null) labReport.setStatus(request.getStatus());
        if (request.getResultDate() != null) labReport.setResultDate(request.getResultDate());

        labReport = labReportRepository.save(labReport);
        return mapToResponse(labReport);
    }

    @Transactional(readOnly = true)
    public Page<LabReportResponse> getPatientLabReports(String clinicId, Long patientId, Pageable pageable) {
        return labReportRepository.findByClinicIdAndPatientIdOrderByTestDateDesc(clinicId, patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<LabReportResponse> getAllLabReports(String clinicId, LabReportStatus status, Pageable pageable) {
        if (status != null) {
            return labReportRepository.findByClinicIdAndStatusOrderByTestDateDesc(clinicId, status, pageable)
                    .map(this::mapToResponse);
        }
        return labReportRepository.findByClinicIdOrderByTestDateDesc(clinicId, pageable)
                .map(this::mapToResponse);
    }

    private LabReportResponse mapToResponse(LabReport l) {
        return LabReportResponse.builder()
                .id(l.getId())
                .visitId(l.getVisit() != null ? l.getVisit().getId() : null)
                .patientId(l.getPatientId())
                .reportNumber(l.getReportNumber())
                .testCategory(l.getTestCategory())
                .testName(l.getTestName())
                .testDate(l.getTestDate())
                .resultDate(l.getResultDate())
                .labName(l.getLabName())
                .orderingDoctor(l.getOrderingDoctor())
                .resultSummary(l.getResultSummary())
                .resultValues(l.getResultValues())
                .interpretation(l.getInterpretation())
                .referenceRange(l.getReferenceRange())
                .isAbnormal(l.getIsAbnormal())
                .notes(l.getNotes())
                .status(l.getStatus())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}

