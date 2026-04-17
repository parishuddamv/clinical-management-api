package com.clinicos.followup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateFollowUpRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String notes;

    private String clinicalNotes;
}
