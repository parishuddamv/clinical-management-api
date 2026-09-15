package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.model.LimitKey;
import com.clinicos.billing.subscription.model.PlanCode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Value
@Builder
public class PlanDto {
    PlanCode code;
    String displayName;
    String description;
    boolean recommended;
    BigDecimal monthlyPrice;
    BigDecimal annualPrice;
    boolean customPricing;
    Set<FeatureCode> features;
    Map<LimitKey, Integer> limits;
}

