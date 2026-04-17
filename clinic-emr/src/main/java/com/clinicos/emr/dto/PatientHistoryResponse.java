package com.clinicos.emr.dto;

import lombok.*;

import java.util.List;

/**
 * Patient EMR History - aggregated timeline view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientHistoryResponse {
    private Long patientId;
    private String clinicId;
    private PatientSummary patientSummary;
    private List<VisitSummary> recentVisits;
    private List<DiagnosisResponse> chronicConditions;
    private List<DiagnosisResponse> recentDiagnoses;
    private List<PrescriptionResponse> recentPrescriptions;
    private List<LabReportResponse> recentLabReports;
    private List<FileAttachmentResponse> recentFiles;
    private VitalSignsResponse latestVitals;
    private long totalVisits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSummary {
        private Long patientId;
        private String patientName;
        private String allergies;
        private String bloodGroup;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitSummary {
        private Long visitId;
        private String visitDate;
        private String visitType;
        private String doctorName;
        private String chiefComplaint;
        private String status;
        private int diagnosisCount;
        private int prescriptionCount;
    }
}

