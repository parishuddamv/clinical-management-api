package com.clinicos.subscription.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentSubscriptionResponse {

    private SubscriptionPlanResponse plan;
    private String status;
    private String billingCycle;
    private LocalDateTime renewalDate;
    private LocalDateTime trialEndsAt;
    private List<SubscriptionFeatureResponse> features;
    private List<SubscriptionUsageResponse> usage;
    private List<Object> addons;
    private List<String> permissions;
}
