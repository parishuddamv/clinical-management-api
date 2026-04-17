package com.clinicos.emr.dto;

import com.clinicos.emr.entity.PatientVisit.VisitType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVisitRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long appointmentId;

    @NotNull(message = "Doctor ID is required")
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
}

