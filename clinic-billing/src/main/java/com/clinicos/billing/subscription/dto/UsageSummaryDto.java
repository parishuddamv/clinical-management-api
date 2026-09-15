package com.clinicos.billing.subscription.dto;

import com.clinicos.billing.subscription.model.PlanCode;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UsageSummaryDto {
    String clinicId;
    PlanCode planCode;
    List<UsageLimitDto> limits;
}

