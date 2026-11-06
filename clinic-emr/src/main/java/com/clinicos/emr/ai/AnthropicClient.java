package com.clinicos.emr.ai;

import com.clinicos.common.exception.RegistrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Minimal client for the Claude Messages API (POST /v1/messages). Configured by environment:
 * AI_ENABLED, ANTHROPIC_API_KEY, AI_MODEL, AI_ANTHROPIC_VERSION. Nothing is sent unless AI is
 * enabled and a key is set. Errors never include patient data in the logs.
 */
@Slf4j
@Component
public class AnthropicClient {

    private final ObjectMapper json;
    private final HttpClient http;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String version;
    private final String baseUrl;
    private final int maxTokens;

    public AnthropicClient(ObjectMapper json,
                           @Value("${ai.enabled:false}") boolean enabled,
                           @Value("${ai.anthropic.api-key:}") String apiKey,
                           @Value("${ai.model:claude-haiku-5-5}") String model,
                           @Value("${ai.anthropic.version:2023-06-01}") String version,
                           @Value("${ai.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
                           @Value("${ai.max-output-tokens:800}") int maxTokens) {
        this.json = json;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.version = version;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.maxTokens = maxTokens;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** True when the clinic deployment has AI switched on and a key configured. */
    public boolean isAvailable() {
        return enabled && !apiKey.isEmpty();
    }

    public String model() {
        return model;
    }

    /** Sends one system prompt and one user message; returns the text of the reply. */
    public String complete(String system, String user) {
        if (!isAvailable()) {
            throw new RegistrationException(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant isn't switched on for this clinic.");
        }
        ObjectNode body = json.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", system);
        body.putArray("messages").addObject().put("role", "user").put("content", user);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/messages"))
                .timeout(Duration.ofSeconds(60))
                .header("x-api-key", apiKey)
                .header("anthropic-version", version)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("AI request failed: {}", e.getClass().getSimpleName());
            throw unavailable();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }

        int status = response.statusCode();
        if (status == 429 || status == 529) {
            log.warn("AI provider is rate-limiting or overloaded (HTTP {})", status);
            throw new RegistrationException(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant is busy. Try again in a minute.");
        }
        if (status / 100 != 2) {
            // The provider's error body describes the request problem, not the patient; keep it short anyway.
            String detail = response.body() == null ? "" : response.body();
            log.warn("AI provider returned HTTP {}: {}", status, detail.length() > 300 ? detail.substring(0, 300) : detail);
            throw unavailable();
        }

        try {
            JsonNode reply = json.readTree(response.body());
            StringBuilder text = new StringBuilder();
            for (JsonNode block : reply.path("content")) {
                if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText());
            }
            if (text.length() == 0) throw unavailable();
            return text.toString().trim();
        } catch (IOException e) {
            log.warn("AI reply could not be read");
            throw unavailable();
        }
    }

    private static RegistrationException unavailable() {
        return new RegistrationException(HttpStatus.BAD_GATEWAY, "The AI assistant couldn't be reached. Try again in a minute.");
    }
}
