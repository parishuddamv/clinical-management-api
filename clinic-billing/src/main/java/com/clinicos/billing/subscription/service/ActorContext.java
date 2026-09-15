package com.clinicos.billing.subscription.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActorContext {
    String email;
    String clinicId;
    String role;
    boolean superAdmin;
}

