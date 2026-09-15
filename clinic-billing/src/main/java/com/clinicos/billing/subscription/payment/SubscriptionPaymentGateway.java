package com.clinicos.billing.subscription.payment;

import com.clinicos.billing.subscription.model.BillingCycle;
import com.clinicos.billing.subscription.model.PlanCode;

import java.math.BigDecimal;

/**
 * Payment gateway abstraction so subscription state changes are decoupled from provider integration.
 */
public interface SubscriptionPaymentGateway {

    PaymentInitiationResult initiatePlanCharge(String clinicId, PlanCode planCode, BillingCycle billingCycle, BigDecimal amount);

    record PaymentInitiationResult(String providerReference, String checkoutUrl, String status) {
    }
}

