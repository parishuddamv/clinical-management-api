package com.clinicos.subscription.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanResponse {

    private String code;
    private String name;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private String currency;
    private Boolean recommended;
    private Long staffSeatLimit;
    private List<SubscriptionFeatureResponse> features;
}
