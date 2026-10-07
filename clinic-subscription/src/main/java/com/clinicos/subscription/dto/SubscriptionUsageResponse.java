package com.clinicos.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionUsageResponse {

    private String code;
    private String name;
    private Long current;
    private Long limit;
    private String unit;
}
