package com.clinicos.subscription.service;

import com.clinicos.subscription.dto.*;
import com.clinicos.subscription.entity.ClinicSubscription;
import com.clinicos.subscription.entity.SubscriptionFeature;
import com.clinicos.subscription.entity.SubscriptionPlan;
import com.clinicos.subscription.entity.SubscriptionUsage;
import com.clinicos.subscription.repository.ClinicSubscriptionRepository;
import com.clinicos.subscription.repository.SubscriptionFeatureRepository;
import com.clinicos.subscription.repository.SubscriptionPlanRepository;
import com.clinicos.subscription.repository.SubscriptionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionFeatureRepository featureRepository;
    private final ClinicSubscriptionRepository subscriptionRepository;
    private final SubscriptionUsageRepository usageRepository;

    public List<SubscriptionPlanResponse> getPlans() {
        return planRepository.findByActiveTrueOrderByIdAsc()
                .stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional
    public CurrentSubscriptionResponse changePlan(
            String clinicId,
            ChangeSubscriptionRequest request) {

        if (clinicId == null || clinicId.isBlank()) {
            throw new IllegalArgumentException("Clinic ID is required");
        }

        if (request == null ||
                request.getPlanCode() == null ||
                request.getPlanCode().isBlank()) {
            throw new IllegalArgumentException("Plan code is required");
        }

        if (request.getBillingCycle() == null ||
                request.getBillingCycle().isBlank()) {
            throw new IllegalArgumentException("Billing cycle is required");
        }

        String planCode = request.getPlanCode().trim().toUpperCase();
        String billingCycle = request.getBillingCycle().trim().toUpperCase();

        if (!"MONTHLY".equals(billingCycle) && !"ANNUAL".equals(billingCycle)) {
            throw new IllegalArgumentException("Billing cycle must be MONTHLY or ANNUAL");
        }

        SubscriptionPlan plan = planRepository.findByCode(planCode)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .orElseThrow(() ->
                        new IllegalArgumentException("Active subscription plan not found: " + planCode));

        ClinicSubscription subscription = subscriptionRepository
                .findByClinicId(clinicId)
                .orElseGet(() -> createStarterTrial(clinicId));

        subscription.setPlan(plan);
        subscription.setBillingCycle(billingCycle);

        subscriptionRepository.save(subscription);

        syncStaffSeatUsage(subscription);

        return getCurrentSubscription(clinicId);
    }

    @Transactional
    public CurrentSubscriptionResponse getCurrentSubscription(String clinicId) {

        if (clinicId == null || clinicId.isBlank()) {
            throw new IllegalArgumentException("Clinic ID is required");
        }

        /*
         * SYSTEM is the internal clinic used by Super Admin.
         * It must not receive a normal clinic subscription.
         */
        if ("SYSTEM".equalsIgnoreCase(clinicId)) {
            return null;
        }

        ClinicSubscription subscription = subscriptionRepository
                .findByClinicId(clinicId)
                .orElseGet(() -> createStarterTrial(clinicId));

        SubscriptionPlan plan = subscription.getPlan();

        List<SubscriptionFeatureResponse> features =
                featureRepository.findByPlanIdOrderByIdAsc(plan.getId())
                        .stream()
                        .map(this::toFeatureResponse)
                        .toList();

        syncStaffSeatUsage(subscription);

        List<SubscriptionUsageResponse> usage =
                usageRepository.findByClinicSubscriptionIdOrderByIdAsc(subscription.getId())
                        .stream()
                        .map(this::toUsageResponse)
                        .toList();

        return CurrentSubscriptionResponse.builder()
                .plan(toPlanResponse(plan))
                .status(subscription.getStatus())
                .billingCycle(subscription.getBillingCycle())
                .renewalDate(subscription.getRenewalDate())
                .trialEndsAt(subscription.getTrialEndsAt())
                .features(features)
                .usage(usage)
                .addons(List.of())
                .permissions(List.of())
                .build();
    }

    private ClinicSubscription createStarterTrial(String clinicId) {

        SubscriptionPlan starterPlan = planRepository
                .findByCode("STARTER")
                .orElseThrow(() ->
                        new IllegalStateException("STARTER subscription plan not found"));

        LocalDateTime now = LocalDateTime.now();

        ClinicSubscription subscription = subscriptionRepository.save(
                ClinicSubscription.builder()
                        .clinicId(clinicId)
                        .plan(starterPlan)
                        .status("TRIAL")
                        .billingCycle("MONTHLY")
                        .trialEndsAt(now.plusDays(14))
                        .renewalDate(now.plusDays(14))
                        .build()
        );

        syncStaffSeatUsage(subscription);

        return subscription;
    }

    private void syncStaffSeatUsage(ClinicSubscription subscription) {

        if (subscription == null ||
                subscription.getId() == null ||
                subscription.getPlan() == null) {
            return;
        }

        SubscriptionPlan plan = subscription.getPlan();

        SubscriptionUsage usage = usageRepository
                .findByClinicSubscriptionIdAndCode(
                        subscription.getId(),
                        "STAFF_SEATS")
                .orElseGet(() ->
                        SubscriptionUsage.builder()
                                .clinicSubscriptionId(subscription.getId())
                                .code("STAFF_SEATS")
                                .name("Staff Seats")
                                .currentValue(0L)
                                .unit("staff")
                                .build()
                );

        usage.setName("Staff Seats");
        usage.setUsageLimit(plan.getStaffSeatLimit());
        usage.setUnit("staff");

        usageRepository.save(usage);
    }

    private SubscriptionUsageResponse toUsageResponse(
            SubscriptionUsage usage) {

        return SubscriptionUsageResponse.builder()
                .code(usage.getCode())
                .name(usage.getName())
                .current(usage.getCurrentValue())
                .limit(usage.getUsageLimit())
                .unit(usage.getUnit())
                .build();
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {

        List<SubscriptionFeatureResponse> features =
                featureRepository.findByPlanIdOrderByIdAsc(plan.getId())
                        .stream()
                        .map(this::toFeatureResponse)
                        .toList();

        return SubscriptionPlanResponse.builder()
                .code(plan.getCode())
                .name(plan.getName())
                .monthlyPrice(plan.getMonthlyPrice())
                .annualPrice(plan.getAnnualPrice())
                .currency(plan.getCurrency())
                .recommended(plan.getRecommended())
                .staffSeatLimit(plan.getStaffSeatLimit())
                .features(features)
                .build();
    }

    private SubscriptionFeatureResponse toFeatureResponse(
            SubscriptionFeature feature) {

        return SubscriptionFeatureResponse.builder()
                .code(feature.getCode())
                .name(feature.getName())
                .included(feature.getIncluded())
                .requiredPlan(feature.getRequiredPlan())
                .message(feature.getMessage())
                .build();
    }
}