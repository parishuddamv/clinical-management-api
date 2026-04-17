package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.*;
import com.clinicos.emr.repository.*;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientHistoryService {

    private final PatientVisitRepository visitRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabReportRepository labReportRepository;
    private final FileAttachmentRepository fileRepository;
    private final VitalSignsRepository vitalSignsRepository;

    @Transactional(readOnly = true)
    public PatientHistoryResponse getPatientHistory(String clinicId, Long patientId) {
        log.info("Fetching patient history for patient: {} in clinic: {}", patientId, clinicId);

        // Get recent visits
        Page<PatientVisit> visitsPage = visitRepository.findByClinicIdAndPatientIdOrderByVisitDateTimeDesc(
                clinicId, patientId, PageRequest.of(0, 10));

        List<PatientHistoryResponse.VisitSummary> visitSummaries = visitsPage.getContent().stream()
                .map(this::mapToVisitSummary)
                .collect(Collectors.toList());

        // Get chronic conditions
        List<DiagnosisResponse> chronicConditions = diagnosisRepository.findChronicDiagnoses(clinicId, patientId)
                .stream().map(this::mapDiagnosisToResponse).collect(Collectors.toList());

        // Get recent diagnoses
        Page<Diagnosis> diagnosesPage = diagnosisRepository.findByClinicIdAndPatientIdOrderByCreatedAtDesc(
                clinicId, patientId, PageRequest.of(0, 10));
        List<DiagnosisResponse> recentDiagnoses = diagnosesPage.getContent().stream()
                .map(this::mapDiagnosisToResponse).collect(Collectors.toList());

        // Get recent prescriptions
        Page<Prescription> prescriptionsPage = prescriptionRepository.findByClinicIdAndPatientIdOrderByPrescriptionDateDesc(
                clinicId, patientId, PageRequest.of(0, 10));
        List<PrescriptionResponse> recentPrescriptions = prescriptionsPage.getContent().stream()
                .map(this::mapPrescriptionToResponse).collect(Collectors.toList());

        // Get recent lab reports
        Page<LabReport> labReportsPage = labReportRepository.findByClinicIdAndPatientIdOrderByTestDateDesc(
                clinicId, patientId, PageRequest.of(0, 10));
        List<LabReportResponse> recentLabReports = labReportsPage.getContent().stream()
                .map(this::mapLabReportToResponse).collect(Collectors.toList());

        // Get recent files
        Page<FileAttachment> filesPage = fileRepository.findByClinicIdAndPatientIdAndIsDeletedFalseOrderByCreatedAtDesc(
                clinicId, patientId, PageRequest.of(0, 10));
        List<FileAttachmentResponse> recentFiles = filesPage.getContent().stream()
                .map(this::mapFileToResponse).collect(Collectors.toList());

        // Get latest vitals
        VitalSignsResponse latestVitals = vitalSignsRepository.findLatestByPatient(clinicId, patientId)
                .map(this::mapVitalsToResponse).orElse(null);

        // Total visit count
        long totalVisits = visitRepository.countByClinicIdAndPatientId(clinicId, patientId);

        return PatientHistoryResponse.builder()
                .patientId(patientId)
                .clinicId(clinicId)
                .recentVisits(visitSummaries)
                .chronicConditions(chronicConditions)
                .recentDiagnoses(recentDiagnoses)
                .recentPrescriptions(recentPrescriptions)
                .recentLabReports(recentLabReports)
                .recentFiles(recentFiles)
                .latestVitals(latestVitals)
                .totalVisits(totalVisits)
                .build();
    }

    @Transactional
    public DiagnosisResponse addDiagnosis(String clinicId, AddDiagnosisRequest request) {
        PatientVisit visit = visitRepository.findByIdAndClinicId(request.getVisitId(), clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + request.getVisitId()));

        Diagnosis diagnosis = Diagnosis.builder()
                .visit(visit)
                .patientId(request.getPatientId())
                .icdCode(request.getIcdCode())
                .diagnosisName(request.getDiagnosisName())
                .diagnosisType(request.getDiagnosisType() != null ? request.getDiagnosisType() : Diagnosis.DiagnosisType.PRIMARY)
                .severity(request.getSeverity())
                .onsetDate(request.getOnsetDate())
                .resolutionDate(request.getResolutionDate())
                .isChronic(request.getIsChronic() != null ? request.getIsChronic() : false)
                .notes(request.getNotes())
                .build();

        diagnosis.setClinicId(clinicId);
        diagnosis = diagnosisRepository.save(diagnosis);

        log.info("Added diagnosis: {} for patient: {}", diagnosis.getDiagnosisName(), request.getPatientId());
        return mapDiagnosisToResponse(diagnosis);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getVisitDiagnoses(String clinicId, Long visitId) {
        // Verify visit belongs to clinic
        visitRepository.findByIdAndClinicId(visitId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));

        return diagnosisRepository.findByVisitIdOrderByDiagnosisTypeAsc(visitId)
                .stream().map(this::mapDiagnosisToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DiagnosisResponse> getPatientDiagnoses(String clinicId, Long patientId, Pageable pageable) {
        return diagnosisRepository.findByClinicIdAndPatientIdOrderByCreatedAtDesc(clinicId, patientId, pageable)
                .map(this::mapDiagnosisToResponse);
    }

    private PatientHistoryResponse.VisitSummary mapToVisitSummary(PatientVisit visit) {
        return PatientHistoryResponse.VisitSummary.builder()
                .visitId(visit.getId())
                .visitDate(visit.getVisitDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")))
                .visitType(visit.getVisitType().name())
                .doctorName(visit.getDoctorName())
                .chiefComplaint(visit.getChiefComplaint())
                .status(visit.getStatus().name())
                .diagnosisCount(visit.getDiagnoses() != null ? visit.getDiagnoses().size() : 0)
                .prescriptionCount(visit.getPrescriptions() != null ? visit.getPrescriptions().size() : 0)
                .build();
    }

    private DiagnosisResponse mapDiagnosisToResponse(Diagnosis d) {
        return DiagnosisResponse.builder()
                .id(d.getId())
                .visitId(d.getVisit().getId())
                .patientId(d.getPatientId())
                .icdCode(d.getIcdCode())
                .diagnosisName(d.getDiagnosisName())
                .diagnosisType(d.getDiagnosisType())
                .severity(d.getSeverity())
                .onsetDate(d.getOnsetDate())
                .resolutionDate(d.getResolutionDate())
                .isChronic(d.getIsChronic())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private PrescriptionResponse mapPrescriptionToResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId())
                .prescriptionNumber(p.getPrescriptionNumber())
                .patientId(p.getPatientId())
                .doctorName(p.getDoctorName())
                .prescriptionDate(p.getPrescriptionDate())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private LabReportResponse mapLabReportToResponse(LabReport l) {
        return LabReportResponse.builder()
                .id(l.getId())
                .testName(l.getTestName())
                .testCategory(l.getTestCategory())
                .testDate(l.getTestDate())
                .resultDate(l.getResultDate())
                .interpretation(l.getInterpretation())
                .isAbnormal(l.getIsAbnormal())
                .status(l.getStatus())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private FileAttachmentResponse mapFileToResponse(FileAttachment f) {
        return FileAttachmentResponse.builder()
                .id(f.getId())
                .patientId(f.getPatientId())
                .fileCategory(f.getFileCategory())
                .originalFileName(f.getOriginalFileName())
                .fileSize(f.getFileSize())
                .description(f.getDescription())
                .downloadUrl("/api/v1/emr/files/" + f.getId() + "/download")
                .createdAt(f.getCreatedAt())
                .build();
    }

    private VitalSignsResponse mapVitalsToResponse(VitalSigns v) {
        return VitalSignsResponse.builder()
                .id(v.getId())
                .patientId(v.getPatientId())
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

