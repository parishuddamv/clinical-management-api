package com.clinicos.subscription.dto;

import lombok.Data;

@Data
public class ChangeSubscriptionRequest {
    private String planCode;
    private String billingCycle;
}
