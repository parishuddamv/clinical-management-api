package com.clinicos.billing.subscription.controller;

import com.clinicos.billing.subscription.dto.*;
import com.clinicos.billing.subscription.model.AddonCode;
import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.service.SubscriptionService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> current(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getCurrentSubscription(authentication)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanDto>>> plans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAvailablePlans()));
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<UsageSummaryDto>> usage(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getCurrentUsage(authentication)));
    }

    @GetMapping("/features/{featureCode}/availability")
    public ResponseEntity<ApiResponse<FeatureAvailabilityDto>> featureAvailability(
            @PathVariable FeatureCode featureCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.checkFeature(authentication, featureCode)));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> upgrade(
            @Valid @RequestBody PlanChangeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Subscription upgraded", subscriptionService.upgrade(authentication, request)));
    }

    @PostMapping("/downgrade")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> downgrade(
            @Valid @RequestBody PlanChangeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Subscription downgraded", subscriptionService.downgrade(authentication, request)));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> cancel(
            @Valid @RequestBody CancellationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled", subscriptionService.cancel(authentication, request.getReason())));
    }

    @PostMapping("/renew")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> renew(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Subscription renewed", subscriptionService.renew(authentication)));
    }

    @GetMapping("/addons")
    public ResponseEntity<ApiResponse<List<AddonDto>>> addons(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAddons(authentication)));
    }

    @PostMapping("/addons/{addonCode}/activate")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> activateAddon(
            @PathVariable AddonCode addonCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Add-on activated", subscriptionService.activateAddon(authentication, addonCode)));
    }

    @PostMapping("/addons/{addonCode}/deactivate")
    public ResponseEntity<ApiResponse<CurrentSubscriptionDto>> deactivateAddon(
            @PathVariable AddonCode addonCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Add-on deactivated", subscriptionService.deactivateAddon(authentication, addonCode)));
    }
}

