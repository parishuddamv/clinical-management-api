package com.clinicos.billing.subscription.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddonUpdateRequest {
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private Boolean active;
}

