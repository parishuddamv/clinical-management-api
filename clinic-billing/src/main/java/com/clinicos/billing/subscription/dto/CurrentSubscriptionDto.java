package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.BillingCycle;
import com.clinicos.billing.subscription.model.PlanCode;
import com.clinicos.billing.subscription.model.SubscriptionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Set;

@Value
@Builder
public class CurrentSubscriptionDto {
    String clinicId;
    PlanCode planCode;
    SubscriptionStatus status;
    BillingCycle billingCycle;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate renewalDate;
    LocalDate trialStartDate;
    LocalDate trialEndDate;
    LocalDate gracePeriodEnd;
    Set<String> activeAddons;
}

