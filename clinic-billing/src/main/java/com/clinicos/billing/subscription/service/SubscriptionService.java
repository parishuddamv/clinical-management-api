package com.clinicos.billing.subscription.service;

import com.clinicos.billing.subscription.config.SubscriptionProperties;
import com.clinicos.billing.subscription.dto.*;
import com.clinicos.billing.subscription.entity.*;
import com.clinicos.billing.subscription.model.*;
import com.clinicos.billing.subscription.repository.*;
import com.clinicos.common.exception.ClinicAccessDeniedException;
import com.clinicos.common.exception.LimitExceededException;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final PlanCode DEFAULT_NEW_CLINIC_PLAN = PlanCode.PROFESSIONAL;

    private final ClinicSubscriptionRepository clinicSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanFeatureRepository planFeatureRepository;
    private final PlanLimitRepository planLimitRepository;
    private final AddonCatalogRepository addonCatalogRepository;
    private final ClinicSubscriptionAddonRepository clinicSubscriptionAddonRepository;
    private final SubscriptionAuditService subscriptionAuditService;
    private final SubscriptionProperties subscriptionProperties;
    private final UsageMeterService usageMeterService;
    private final RolePermissionService rolePermissionService;

    @Transactional
    public CurrentSubscriptionDto getCurrentSubscription(Authentication authentication) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        ClinicSubscription subscription = getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole());
        return toCurrentDto(subscription);
    }

    @Transactional(readOnly = true)
    public List<PlanDto> getAvailablePlans() {
        return subscriptionPlanRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional
    public UsageSummaryDto getCurrentUsage(Authentication authentication) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        ClinicSubscription subscription = getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole());
        List<UsageLimitDto> limits = planLimitRepository.findByPlanCode(subscription.getPlanCode()).stream()
                .map(limit -> {
                    long usage = usageMeterService.getCurrentUsage(actor.getClinicId(), limit.getLimitKey());
                    Integer max = limit.getMaxValue();
                    boolean exceeded = max != null && usage > max;
                    return UsageLimitDto.builder()
                            .limitKey(limit.getLimitKey())
                            .currentUsage(usage)
                            .allowedMaximum(max)
                            .exceeded(exceeded)
                            .build();
                })
                .toList();

        return UsageSummaryDto.builder()
                .clinicId(actor.getClinicId())
                .planCode(subscription.getPlanCode())
                .limits(limits)
                .build();
    }

    @Transactional
    public FeatureAvailabilityDto checkFeature(Authentication authentication, FeatureCode featureCode) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        ClinicSubscription subscription = getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole());
        boolean subscriptionValid = isSubscriptionValid(subscription);
        boolean featureAvailable = subscriptionValid && hasFeature(actor.getClinicId(), subscription.getPlanCode(), featureCode);

        String reason;
        if (!subscriptionValid) {
            reason = "Subscription status is " + subscription.getStatus();
        } else if (!featureAvailable) {
            reason = "Feature not available in current plan/add-ons";
        } else {
            reason = "Feature is available";
        }

        return FeatureAvailabilityDto.builder()
                .clinicId(actor.getClinicId())
                .planCode(subscription.getPlanCode())
                .featureCode(featureCode)
                .available(featureAvailable)
                .reason(reason)
                .build();
    }

    @Transactional
    public CurrentSubscriptionDto upgrade(Authentication authentication, PlanChangeRequest request) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        rolePermissionService.requirePermission(actor.getClinicId(), actor.getRole(), "MANAGE_SETTINGS");
        return changePlan(actor, actor.getClinicId(), request, false);
    }

    @Transactional
    public CurrentSubscriptionDto downgrade(Authentication authentication, PlanChangeRequest request) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        rolePermissionService.requirePermission(actor.getClinicId(), actor.getRole(), "MANAGE_SETTINGS");
        return changePlan(actor, actor.getClinicId(), request, true);
    }

    @Transactional
    public CurrentSubscriptionDto platformChangePlan(String clinicId, PlanChangeRequest request, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);
        return changePlan(actor, clinicId, request, false);
    }


    @Transactional
    public CurrentSubscriptionDto cancel(Authentication authentication, String reason) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        rolePermissionService.requirePermission(actor.getClinicId(), actor.getRole(), "MANAGE_SETTINGS");

        ClinicSubscription subscription = getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole());
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(java.time.LocalDateTime.now());
        subscription.setCancelledBy(actor.getEmail());
        subscription.setCancellationReason(reason);
        clinicSubscriptionRepository.save(subscription);

        subscriptionAuditService.record(
                actor.getClinicId(),
                SubscriptionAction.SUBSCRIPTION_CANCELLED,
                actor.getEmail(),
                actor.getRole(),
                "Cancellation reason: " + reason
        );

        return toCurrentDto(subscription);
    }

    @Transactional
    public CurrentSubscriptionDto renew(Authentication authentication) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        rolePermissionService.requirePermission(actor.getClinicId(), actor.getRole(), "MANAGE_SETTINGS");

        ClinicSubscription subscription = getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole());
        LocalDate from = LocalDate.now();
        int months = subscription.getBillingCycle() == BillingCycle.ANNUAL ? 12 : 1;

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(from);
        subscription.setEndDate(from.plusMonths(months));
        subscription.setRenewalDate(subscription.getEndDate());
        subscription.setCancelledAt(null);
        subscription.setCancelledBy(null);
        subscription.setCancellationReason(null);
        clinicSubscriptionRepository.save(subscription);

        subscriptionAuditService.record(
                actor.getClinicId(),
                SubscriptionAction.SUBSCRIPTION_RENEWED,
                actor.getEmail(),
                actor.getRole(),
                "Subscription renewed"
        );

        return toCurrentDto(subscription);
    }

    @Transactional(readOnly = true)
    public List<AddonDto> getAddons(Authentication authentication) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        Set<AddonCode> active = clinicSubscriptionAddonRepository.findByClinicIdAndActiveTrue(actor.getClinicId()).stream()
                .map(ClinicSubscriptionAddon::getAddonCode)
                .collect(Collectors.toSet());

        return addonCatalogRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(addon -> AddonDto.builder()
                        .code(addon.getCode())
                        .displayName(addon.getDisplayName())
                        .featureCode(addon.getFeatureCode())
                        .monthlyPrice(addon.getMonthlyPrice())
                        .annualPrice(addon.getAnnualPrice())
                        .activeForClinic(active.contains(addon.getCode()))
                        .build())
                .toList();
    }

    @Transactional
    public CurrentSubscriptionDto activateAddon(Authentication authentication, AddonCode addonCode) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        rolePermissionService.requirePermission(actor.getClinicId(), actor.getRole(), "MANAGE_SETTINGS");

        addonCatalogRepository.findByCode(addonCode)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on not found: " + addonCode));

        ClinicSubscriptionAddon addon = clinicSubscriptionAddonRepository
                .findByClinicIdAndAddonCode(actor.getClinicId(), addonCode)
                .orElseGet(() -> {
                    ClinicSubscriptionAddon created = ClinicSubscriptionAddon.builder()
                            .addonCode(addonCode)
                            .active(true)
                            .startDate(LocalDate.now())
                            .build();
                    created.setClinicId(actor.getClinicId());
                    return created;
                });

        addon.setActive(true);
        addon.setEndDate(null);
        clinicSubscriptionAddonRepository.save(addon);

        subscriptionAuditService.record(
                actor.getClinicId(),
                SubscriptionAction.ADDON_ACTIVATED,
                actor.getEmail(),
                actor.getRole(),
                "Activated add-on " + addonCode
        );
        return toCurrentDto(getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole()));
    }

    @Transactional
    public CurrentSubscriptionDto deactivateAddon(Authentication authentication, AddonCode addonCode) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        rolePermissionService.requirePermission(actor.getClinicId(), actor.getRole(), "MANAGE_SETTINGS");

        ClinicSubscriptionAddon addon = clinicSubscriptionAddonRepository
                .findByClinicIdAndAddonCode(actor.getClinicId(), addonCode)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on not active: " + addonCode));

        addon.setActive(false);
        addon.setEndDate(LocalDate.now());
        clinicSubscriptionAddonRepository.save(addon);

        subscriptionAuditService.record(
                actor.getClinicId(),
                SubscriptionAction.ADDON_DEACTIVATED,
                actor.getEmail(),
                actor.getRole(),
                "Deactivated add-on " + addonCode
        );
        return toCurrentDto(getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole()));
    }

    @Transactional
    public CurrentSubscriptionDto extendTrial(String clinicId, int extraDays, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);
        ClinicSubscription subscription = getOrCreateSubscription(clinicId, actor.getEmail(), actor.getRole());
        if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
            throw new ClinicAccessDeniedException("Trial can be extended only for TRIAL subscriptions");
        }

        subscription.setTrialEndDate(subscription.getTrialEndDate().plusDays(extraDays));
        subscription.setEndDate(subscription.getTrialEndDate());
        clinicSubscriptionRepository.save(subscription);
        subscriptionAuditService.record(
                clinicId,
                SubscriptionAction.TRIAL_EXTENDED,
                actor.getEmail(),
                actor.getRole(),
                "Extended trial by " + extraDays + " days"
        );
        return toCurrentDto(subscription);
    }

    @Transactional
    public CurrentSubscriptionDto suspendClinic(String clinicId, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);
        ClinicSubscription subscription = getOrCreateSubscription(clinicId, actor.getEmail(), actor.getRole());
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        clinicSubscriptionRepository.save(subscription);
        subscriptionAuditService.record(clinicId, SubscriptionAction.CLINIC_SUSPENDED, actor.getEmail(), actor.getRole(), "Clinic suspended");
        return toCurrentDto(subscription);
    }

    @Transactional
    public CurrentSubscriptionDto reactivateClinic(String clinicId, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);
        ClinicSubscription subscription = getOrCreateSubscription(clinicId, actor.getEmail(), actor.getRole());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        clinicSubscriptionRepository.save(subscription);
        subscriptionAuditService.record(clinicId, SubscriptionAction.CLINIC_REACTIVATED, actor.getEmail(), actor.getRole(), "Clinic reactivated");
        return toCurrentDto(subscription);
    }

    @Transactional
    public PlanDto updatePlanPricing(PlanCode planCode, PlanPricingUpdateRequest request, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);

        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planCode));

        if (request.getMonthlyPrice() != null) {
            plan.setMonthlyPrice(request.getMonthlyPrice());
        }
        if (request.getAnnualPrice() != null) {
            plan.setAnnualPrice(request.getAnnualPrice());
        }
        if (request.getCustomPricing() != null) {
            plan.setCustomPricing(request.getCustomPricing());
        }
        if (request.getRecommended() != null) {
            plan.setRecommended(request.getRecommended());
        }
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        subscriptionPlanRepository.save(plan);
        return toPlanDto(plan);
    }

    @Transactional
    public PlanDto updatePlanLimit(PlanCode planCode, LimitKey limitKey, PlanLimitUpdateRequest request, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);

        PlanLimit planLimit = planLimitRepository.findByPlanCodeAndLimitKey(planCode, limitKey)
                .orElseThrow(() -> new ResourceNotFoundException("Plan limit not found: " + planCode + " / " + limitKey));
        planLimit.setMaxValue(request.getMaxValue());
        planLimitRepository.save(planLimit);

        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planCode));
        return toPlanDto(plan);
    }

    @Transactional
    public PlanDto updatePlanFeature(PlanCode planCode, FeatureCode featureCode, boolean enabled, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);

        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planCode));

        Optional<PlanFeature> existing = planFeatureRepository.findByPlanCodeAndFeatureCode(planCode, featureCode);
        if (enabled && existing.isEmpty()) {
            planFeatureRepository.save(PlanFeature.builder().plan(plan).featureCode(featureCode).build());
        }
        if (!enabled) {
            existing.ifPresent(planFeatureRepository::delete);
        }

        subscriptionAuditService.record("PLATFORM", SubscriptionAction.FEATURE_OVERRIDE_CHANGED, actor.getEmail(), actor.getRole(),
                "Plan " + planCode + " feature " + featureCode + " set to " + enabled);
        return toPlanDto(plan);
    }

    @Transactional
    public AddonDto updateAddonCatalog(AddonCode addonCode, AddonUpdateRequest request, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);

        AddonCatalog addon = addonCatalogRepository.findByCode(addonCode)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on not found: " + addonCode));

        if (request.getMonthlyPrice() != null) {
            addon.setMonthlyPrice(request.getMonthlyPrice());
        }
        if (request.getAnnualPrice() != null) {
            addon.setAnnualPrice(request.getAnnualPrice());
        }
        if (request.getActive() != null) {
            addon.setActive(request.getActive());
        }

        addonCatalogRepository.save(addon);
        return AddonDto.builder()
                .code(addon.getCode())
                .displayName(addon.getDisplayName())
                .featureCode(addon.getFeatureCode())
                .monthlyPrice(addon.getMonthlyPrice())
                .annualPrice(addon.getAnnualPrice())
                .activeForClinic(false)
                .build();
    }

    @Transactional
    public CurrentSubscriptionDto getByClinic(String clinicId, ActorContext actor) {
        rolePermissionService.requirePlatformAdmin(actor);
        ClinicSubscription subscription = getOrCreateSubscription(clinicId, actor.getEmail(), actor.getRole());
        return toCurrentDto(subscription);
    }

    @Transactional
    public void enforceAccess(Authentication authentication, FeatureCode featureCode, String requiredPermission) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        enforceAccess(actor.getClinicId(), actor.getEmail(), actor.getRole(), featureCode, requiredPermission);
    }

    @Transactional
    public void enforceUsageLimit(Authentication authentication, LimitKey limitKey) {
        ActorContext actor = rolePermissionService.resolveActor(authentication);
        ClinicSubscription subscription = getOrCreateSubscription(actor.getClinicId(), actor.getEmail(), actor.getRole());
        assertLimit(subscription, actor.getClinicId(), limitKey);
    }

    @Transactional(readOnly = true)
    public ActorContext actor(Authentication authentication) {
        return rolePermissionService.resolveActor(authentication);
    }

    @Transactional
    public void enforceAccess(String clinicId, String actorEmail, String role, FeatureCode featureCode, String requiredPermission) {
        ClinicSubscription subscription = getOrCreateSubscription(clinicId, actorEmail, role);
        if (!isSubscriptionValid(subscription)) {
            throw new ClinicAccessDeniedException("Subscription status " + subscription.getStatus() + " does not allow this operation");
        }

        boolean featureAvailable = hasFeature(clinicId, subscription.getPlanCode(), featureCode);
        if (!featureAvailable) {
            throw new ClinicAccessDeniedException("Feature " + featureCode + " is not available for plan " + subscription.getPlanCode());
        }

        rolePermissionService.requirePermission(clinicId, role, requiredPermission);
    }

    private boolean isSubscriptionValid(ClinicSubscription subscription) {
        return switch (subscription.getStatus()) {
            case TRIAL, ACTIVE, PAST_DUE, GRACE_PERIOD -> true;
            case SUSPENDED, CANCELLED, EXPIRED -> false;
        };
    }

    private boolean hasFeature(String clinicId, PlanCode planCode, FeatureCode featureCode) {
        Set<FeatureCode> planFeatures = planFeatureRepository.findFeatureCodesByPlan(planCode);
        if (planFeatures.contains(featureCode)) {
            return true;
        }

        Set<FeatureCode> addonFeatures = clinicSubscriptionAddonRepository.findByClinicIdAndActiveTrue(clinicId).stream()
                .map(ClinicSubscriptionAddon::getAddonCode)
                .map(this::featureForAddon)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return addonFeatures.contains(featureCode);
    }

    private FeatureCode featureForAddon(AddonCode addonCode) {
        return switch (addonCode) {
            case WHATSAPP -> FeatureCode.WHATSAPP;
            case PHARMACY -> FeatureCode.PHARMACY;
            case LAB -> FeatureCode.LAB_MANAGEMENT;
            case MULTI_LOCATION -> FeatureCode.MULTIPLE_LOCATIONS;
            case ADVANCED_ANALYTICS -> FeatureCode.ADVANCED_ANALYTICS;
            case TELEMEDICINE -> FeatureCode.TELEMEDICINE;
            case API_INTEGRATIONS -> FeatureCode.API_INTEGRATIONS;
        };
    }

    private CurrentSubscriptionDto changePlan(ActorContext actor, String clinicId, PlanChangeRequest request, boolean isDowngrade) {
        ClinicSubscription subscription = getOrCreateSubscription(clinicId, actor.getEmail(), actor.getRole());
        if (isDowngrade) {
            assertPlanDowngradePossible(subscription, request.getPlanCode(), clinicId);
        }

        int months = request.getBillingCycle() == BillingCycle.ANNUAL ? 12 : 1;
        LocalDate start = LocalDate.now();

        subscription.setPlanCode(request.getPlanCode());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(start);
        subscription.setEndDate(start.plusMonths(months));
        subscription.setRenewalDate(subscription.getEndDate());
        subscription.setTrialStartDate(null);
        subscription.setTrialEndDate(null);

        clinicSubscriptionRepository.save(subscription);
        subscriptionAuditService.record(
                clinicId,
                SubscriptionAction.PLAN_CHANGED,
                actor.getEmail(),
                actor.getRole(),
                "Changed plan to " + request.getPlanCode() + " with " + request.getBillingCycle() + " billing"
        );
        return toCurrentDto(subscription);
    }

    private void assertPlanDowngradePossible(ClinicSubscription currentSubscription, PlanCode targetPlanCode, String clinicId) {
        List<PlanLimit> targetLimits = planLimitRepository.findByPlanCode(targetPlanCode);
        for (PlanLimit limit : targetLimits) {
            long usage = usageMeterService.getCurrentUsage(clinicId, limit.getLimitKey());
            Integer max = limit.getMaxValue();
            if (max != null && usage > max) {
                throw new LimitExceededException(
                        limit.getLimitKey().name(),
                        usage,
                        max,
                        currentSubscription.getPlanCode().name(),
                        recommendUpgrade(currentSubscription.getPlanCode()),
                        "Cannot downgrade because usage exceeds target plan limit"
                );
            }
        }
    }

    private void assertLimit(ClinicSubscription subscription, String clinicId, LimitKey limitKey) {
        PlanLimit planLimit = planLimitRepository.findByPlanCodeAndLimitKey(subscription.getPlanCode(), limitKey)
                .orElseThrow(() -> new ResourceNotFoundException("Limit not defined for " + limitKey));

        Integer max = planLimit.getMaxValue();
        if (max == null) {
            return;
        }

        long usage = usageMeterService.getCurrentUsage(clinicId, limitKey);
        if (usage >= max) {
            throw new LimitExceededException(
                    limitKey.name(),
                    usage,
                    max,
                    subscription.getPlanCode().name(),
                    recommendUpgrade(subscription.getPlanCode()),
                    "Plan limit reached for " + limitKey
            );
        }
    }

    private String recommendUpgrade(PlanCode currentPlan) {
        return switch (currentPlan) {
            case STARTER -> PlanCode.PROFESSIONAL.name();
            case PROFESSIONAL -> PlanCode.BUSINESS.name();
            case BUSINESS -> PlanCode.ENTERPRISE.name();
            case ENTERPRISE -> "Contact support";
        };
    }

    private ClinicSubscription getOrCreateSubscription(String clinicId, String actorEmail, String actorRole) {
        return clinicSubscriptionRepository.findByClinicId(clinicId)
                .map(this::resolveExpiredOrTrialState)
                .orElseGet(() -> createTrialSubscription(clinicId, actorEmail, actorRole));
    }

    private ClinicSubscription resolveExpiredOrTrialState(ClinicSubscription subscription) {
        LocalDate today = LocalDate.now();

        if (subscription.getStatus() == SubscriptionStatus.TRIAL && subscription.getTrialEndDate() != null && today.isAfter(subscription.getTrialEndDate())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            clinicSubscriptionRepository.save(subscription);
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE && today.isAfter(subscription.getEndDate())) {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            if (subscription.getGracePeriodEnd() == null) {
                subscription.setGracePeriodEnd(today.plusDays(subscriptionProperties.getGraceDays()));
            }
            clinicSubscriptionRepository.save(subscription);
        }

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE && subscription.getGracePeriodEnd() != null && today.isAfter(subscription.getGracePeriodEnd())) {
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            clinicSubscriptionRepository.save(subscription);
        }

        return subscription;
    }

    private ClinicSubscription createTrialSubscription(String clinicId, String actorEmail, String actorRole) {
        LocalDate today = LocalDate.now();
        ClinicSubscription subscription = ClinicSubscription.builder()
                .planCode(DEFAULT_NEW_CLINIC_PLAN)
                .status(SubscriptionStatus.TRIAL)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(today)
                .trialStartDate(today)
                .trialEndDate(today.plusDays(subscriptionProperties.getTrialDays()))
                .endDate(today.plusDays(subscriptionProperties.getTrialDays()))
                .renewalDate(today.plusDays(subscriptionProperties.getTrialDays()))
                .gracePeriodEnd(today.plusDays(subscriptionProperties.getTrialDays() + subscriptionProperties.getGraceDays()))
                .build();
        subscription.setClinicId(clinicId);
        ClinicSubscription saved = clinicSubscriptionRepository.save(subscription);

        subscriptionAuditService.record(
                clinicId,
                SubscriptionAction.SUBSCRIPTION_CREATED,
                actorEmail,
                actorRole,
                "Auto-created default trial subscription"
        );

        return saved;
    }

    private CurrentSubscriptionDto toCurrentDto(ClinicSubscription subscription) {
        Set<String> activeAddons = clinicSubscriptionAddonRepository.findByClinicIdAndActiveTrue(subscription.getClinicId()).stream()
                .map(addon -> addon.getAddonCode().name())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return CurrentSubscriptionDto.builder()
                .clinicId(subscription.getClinicId())
                .planCode(subscription.getPlanCode())
                .status(subscription.getStatus())
                .billingCycle(subscription.getBillingCycle())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .renewalDate(subscription.getRenewalDate())
                .trialStartDate(subscription.getTrialStartDate())
                .trialEndDate(subscription.getTrialEndDate())
                .gracePeriodEnd(subscription.getGracePeriodEnd())
                .activeAddons(activeAddons)
                .build();
    }

    private PlanDto toPlanDto(SubscriptionPlan plan) {
        Map<LimitKey, Integer> limits = new LinkedHashMap<>();
        for (PlanLimit limit : planLimitRepository.findByPlanCode(plan.getCode())) {
            limits.put(limit.getLimitKey(), limit.getMaxValue());
        }

        return PlanDto.builder()
                .code(plan.getCode())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .recommended(Boolean.TRUE.equals(plan.getRecommended()))
                .monthlyPrice(plan.getMonthlyPrice())
                .annualPrice(plan.getAnnualPrice())
                .customPricing(Boolean.TRUE.equals(plan.getCustomPricing()))
                .features(planFeatureRepository.findFeatureCodesByPlan(plan.getCode()))
                .limits(limits)
                .build();
    }
}

