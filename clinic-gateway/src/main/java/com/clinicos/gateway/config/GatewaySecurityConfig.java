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
 * 
 * Note: CORS configuration is handled by CorsConfig class
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * Security filter chain for reactive applications (Spring Cloud Gateway uses WebFlux)
     */
    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http) {
        http
                // CSRF configuration
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Authorization rules
                .authorizeExchange(exchange -> exchange
                        // Allow unauthenticated access to health and info endpoints
                        .pathMatchers("/health", "/info", "/actuator/**").permitAll()
                        // Allow unauthenticated access to public API endpoints
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/v1/public/**").permitAll()
                        // Allow OPTIONS requests for CORS preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Route-level JWT checks are enforced via JwtGatewayFilterFactory
                        .anyExchange().permitAll());

        return http.build();
    }
}

