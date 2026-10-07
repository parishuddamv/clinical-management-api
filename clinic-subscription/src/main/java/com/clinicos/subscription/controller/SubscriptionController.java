package com.clinicos.subscription.controller;

import com.clinicos.common.entity.TenantContext;
import com.clinicos.subscription.dto.CurrentSubscriptionResponse;
import com.clinicos.subscription.dto.ChangeSubscriptionRequest;
import com.clinicos.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", subscriptionService.getPlans()
                )
        );
    }

    @PostMapping("/change-plan")
    public ResponseEntity<?> changePlan(
            @RequestBody ChangeSubscriptionRequest request) {

        String clinicId = TenantContext.getClinicId();

        if (clinicId == null || clinicId.isBlank()) {
            return ResponseEntity.status(401).body(
                    Map.of(
                            "success", false,
                            "message", "Clinic context not found"
                    )
            );
        }

        CurrentSubscriptionResponse subscription =
                subscriptionService.changePlan(clinicId, request);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", subscription
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentSubscription() {

        String clinicId = TenantContext.getClinicId();

        if (clinicId == null || clinicId.isBlank()) {
            return ResponseEntity.status(401).body(
                    Map.of(
                            "success", false,
                            "message", "Clinic context not found"
                    )
            );
        }

        CurrentSubscriptionResponse subscription =
                subscriptionService.getCurrentSubscription(clinicId);

        if (subscription == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", null);
            response.put("message", "No subscription for this clinic");

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", subscription
                )
        );
    }
}
