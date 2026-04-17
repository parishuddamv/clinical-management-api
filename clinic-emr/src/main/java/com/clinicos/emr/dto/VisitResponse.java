package com.clinicos.emr.dto;

import com.clinicos.emr.entity.PatientVisit.VisitStatus;
import com.clinicos.emr.entity.PatientVisit.VisitType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitResponse {
    private Long id;
    private String clinicId;
    private Long patientId;
    private Long appointmentId;
    private String doctorId;
    private String doctorName;
    private LocalDateTime visitDateTime;
    private VisitType visitType;
    private String chiefComplaint;
    private String presentIllness;
    private String pastMedicalHistory;
    private String familyHistory;
    private String socialHistory;
    private String allergies;
    private Map<String, Object> vitalSigns;
    private String physicalExamination;
    private String clinicalNotes;
    private String treatmentPlan;
    private String followUpInstructions;
    private VisitStatus status;
    private List<DiagnosisResponse> diagnoses;
    private List<PrescriptionResponse> prescriptions;
    private List<LabReportResponse> labReports;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

