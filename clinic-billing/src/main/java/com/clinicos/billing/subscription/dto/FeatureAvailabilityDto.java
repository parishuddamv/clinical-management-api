package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.model.PlanCode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeatureAvailabilityDto {
    String clinicId;
    PlanCode planCode;
    FeatureCode featureCode;
    boolean available;
    String reason;
}

