package com.clinicos.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway Security Configuration for the API Gateway.
 * Handles:
 * - Authorization rules
 * - CSRF protection
 * Note: CORS configuration is handled by CorsConfig class
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * Security filter chain for reactive applications (Spring Cloud Gateway uses WebFlux)
     */
    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http,
            com.clinicos.common.security.RegistrationAccess registrationAccess,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        http
                // Do not enable .cors() here; CorsWebFilter already handles CORS globally for the gateway.
                // CSRF configuration
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((exchange, ex) -> forbidden(exchange, objectMapper))
                        .accessDeniedHandler((exchange, ex) -> forbidden(exchange, objectMapper)))
                // Authorization rules
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/v1/auth/admin/**").access((authentication, context) ->
                                reactor.core.publisher.Mono.fromCallable(() -> {
                                    registrationAccess.requireSuperAdmin(context.getExchange().getRequest()
                                            .getHeaders().getFirst("Authorization"));
                                    return new org.springframework.security.authorization.AuthorizationDecision(true);
                                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                                .onErrorReturn(new org.springframework.security.authorization.AuthorizationDecision(false)))
                        // Allow unauthenticated access to health and info endpoints
                        .pathMatchers("/health", "/info", "/actuator/**").permitAll()
                        // Allow unauthenticated access to public API endpoints
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/v1/public/**").permitAll()
                        // Route-level JWT checks are enforced via JwtGatewayFilterFactory
                        .anyExchange().permitAll());

        return http.build();
    }

    private reactor.core.publisher.Mono<Void> forbidden(org.springframework.web.server.ServerWebExchange exchange,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(reactor.core.publisher.Mono.fromCallable(() ->
                exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(
                        com.clinicos.common.dto.ApiResponse.error("Super Admin authorization is required.")))));
    }
}

