package com.clinicos.staff.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SubscriptionClient {

    @Value("${services.subscription.url}")
    private String subscriptionServiceUrl;

    private final ObjectMapper objectMapper;

    public SubscriptionInfo getCurrentSubscription(String authorizationHeader) {

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }

        RestClient client = RestClient.builder()
                .baseUrl(subscriptionServiceUrl)
                .build();

        String response = client.get()
                .uri("/subscriptions/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");

            if (data == null || data.isNull()) {
                return new SubscriptionInfo(null, null);
            }

            String status = data.hasNonNull("status")
                    ? data.get("status").asText()
                    : null;

            Long staffSeatLimit = null;

            JsonNode plan = data.get("plan");
            if (plan != null && !plan.isNull() && plan.hasNonNull("staffSeatLimit")) {
                staffSeatLimit = plan.get("staffSeatLimit").asLong();
            }

            return new SubscriptionInfo(staffSeatLimit, status);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to read subscription service response", e);
        }
    }

    public record SubscriptionInfo(
            Long staffSeatLimit,
            String status
    ) {
    }
}