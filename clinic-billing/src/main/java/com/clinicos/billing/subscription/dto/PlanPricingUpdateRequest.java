package com.clinicos.billing.subscription.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanPricingUpdateRequest {
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private Boolean customPricing;
    private Boolean recommended;
    private Boolean active;
}

