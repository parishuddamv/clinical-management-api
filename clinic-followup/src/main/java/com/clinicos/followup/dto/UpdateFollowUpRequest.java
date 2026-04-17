package com.clinicos.followup.dto;

import com.clinicos.followup.entity.FollowUp.FollowUpStatus;
import lombok.Data;

@Data
public class UpdateFollowUpRequest {

    private FollowUpStatus status;

    private String notes;

    private String clinicalNotes;
}
