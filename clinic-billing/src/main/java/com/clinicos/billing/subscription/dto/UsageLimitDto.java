package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.LimitKey;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UsageLimitDto {
    LimitKey limitKey;
    long currentUsage;
    Integer allowedMaximum;
    boolean exceeded;
}

