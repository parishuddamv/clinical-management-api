package com.clinicos.followup.dto;

import com.clinicos.followup.entity.FollowUp;
import com.clinicos.followup.entity.FollowUp.FollowUpStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FollowUpResponse {

    private Long id;
    private Long patientId;
    private LocalDate dueDate;
    private FollowUpStatus status;
    private String notes;
    private String clinicalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FollowUpResponse from(FollowUp followUp) {
        return FollowUpResponse.builder()
                .id(followUp.getId())
                .patientId(followUp.getPatientId())
                .dueDate(followUp.getDueDate())
                .status(followUp.getStatus())
                .notes(followUp.getNotes())
                .clinicalNotes(followUp.getClinicalNotes())
                .createdAt(followUp.getCreatedAt())
                .updatedAt(followUp.getUpdatedAt())
                .build();
    }
}
