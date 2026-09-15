package com.clinicos.billing.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancellationRequest {
    @NotBlank
    private String reason;
}

