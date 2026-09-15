package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.BillingCycle;
import com.clinicos.billing.subscription.model.PlanCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanChangeRequest {
    @NotNull
    private PlanCode planCode;

    @NotNull
    private BillingCycle billingCycle;
}

