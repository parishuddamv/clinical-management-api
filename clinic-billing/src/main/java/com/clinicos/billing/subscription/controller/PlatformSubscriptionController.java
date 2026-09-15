package com.clinicos.billing.subscription.controller;

import com.clinicos.billing.subscription.dto.CurrentSubscriptionDto;
import com.clinicos.billing.subscription.dto.AddonDto;
import com.clinicos.billing.subscription.dto.AddonUpdateRequest;
import com.clinicos.billing.subscription.dto.PlanDto;
import com.clinicos.billing.subscription.dto.PlanChangeRequest;
import com.clinicos.billing.subscription.dto.PlanLimitUpdateRequest;
import com.clinicos.billing.subscription.dto.PlanPricingUpdateRequest;
import com.clinicos.billing.subscription.model.AddonCode;
import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.model.LimitKey;
import com.clinicos.billing.subscription.model.PlanCode;
import com.clinicos.billing.subscription.service.ActorContext;
import com.clinicos.billing.subscription.service.SubscriptionService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/subscriptions")
@RequiredArgsConstructor
public class PlatformSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/clinics/{clinicId}")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> getByClinic(
            @PathVariable String clinicId,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getByClinic(clinicId, actor)));
    }

    @PostMapping("/clinics/{clinicId}/plan")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> changePlan(
            @PathVariable String clinicId,
            @Valid @RequestBody PlanChangeRequest request,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.platformChangePlan(clinicId, request, actor)));
    }

    @PostMapping("/clinics/{clinicId}/trial/extend")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> extendTrial(
            @PathVariable String clinicId,
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.extendTrial(clinicId, days, actor)));
    }

    @PostMapping("/clinics/{clinicId}/suspend")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> suspendClinic(
            @PathVariable String clinicId,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.suspendClinic(clinicId, actor)));
    }

    @PostMapping("/clinics/{clinicId}/reactivate")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> reactivateClinic(
            @PathVariable String clinicId,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.reactivateClinic(clinicId, actor)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanDto>>> getPlans(Authentication authentication) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAvailablePlans()));
    }

    @PatchMapping("/plans/{planCode}/pricing")
    public ResponseEntity<ApiResponse<PlanDto>> updatePlanPricing(
            @PathVariable PlanCode planCode,
            @RequestBody PlanPricingUpdateRequest request,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.updatePlanPricing(planCode, request, actor)));
    }

    @PatchMapping("/plans/{planCode}/limits/{limitKey}")
    public ResponseEntity<ApiResponse<PlanDto>> updatePlanLimit(
            @PathVariable PlanCode planCode,
            @PathVariable LimitKey limitKey,
            @RequestBody PlanLimitUpdateRequest request,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.updatePlanLimit(planCode, limitKey, request, actor)));
    }

    @PatchMapping("/plans/{planCode}/features/{featureCode}")
    public ResponseEntity<ApiResponse<PlanDto>> updatePlanFeature(
            @PathVariable PlanCode planCode,
            @PathVariable FeatureCode featureCode,
            @RequestParam boolean enabled,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.updatePlanFeature(planCode, featureCode, enabled, actor)));
    }

    @PatchMapping("/addons/{addonCode}")
    public ResponseEntity<ApiResponse<AddonDto>> updateAddon(
            @PathVariable AddonCode addonCode,
            @RequestBody AddonUpdateRequest request,
            Authentication authentication
    ) {
        ActorContext actor = subscriptionService.actor(authentication);
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.updateAddonCatalog(addonCode, request, actor)));
    }
}

