package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.AddonCode;
import com.clinicos.billing.subscription.model.FeatureCode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AddonDto {
    AddonCode code;
    String displayName;
    FeatureCode featureCode;
    BigDecimal monthlyPrice;
    BigDecimal annualPrice;
    boolean activeForClinic;
}

