package com.clinicos.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionFeatureResponse {

    private String code;
    private String name;
    private Boolean included;
    private String requiredPlan;
    private String message;
}
