package com.clinicos.billing.subscription.payment;

import com.clinicos.billing.subscription.model.BillingCycle;
import com.clinicos.billing.subscription.model.PlanCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NoopSubscriptionPaymentGateway implements SubscriptionPaymentGateway {

    @Override
    public PaymentInitiationResult initiatePlanCharge(String clinicId, PlanCode planCode, BillingCycle billingCycle, BigDecimal amount) {
        // Placeholder for Razorpay/UPI/Card integration.
        return new PaymentInitiationResult(null, null, "NOT_CONFIGURED");
    }
}

