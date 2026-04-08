package com.clinicos.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration for microservice routing
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtGatewayFilterFactory jwtGatewayFilterFactory;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Patient Service Routes
                .route("patient-service", r -> r
                        .path("/api/v1/patients/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri("http://clinic-patient-service:8081"))

                // Appointment Service Routes
                .route("appointment-service", r -> r
                        .path("/api/v1/appointments/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri("http://clinic-appointment-service:8082"))

                // Follow-up Service Routes
                .route("followup-service", r -> r
                        .path("/api/v1/followups/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri("http://clinic-followup-service:8083"))

                // Notification Service Routes
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri("http://clinic-notification-service:8084"))

                // Billing Service Routes
                .route("billing-service", r -> r
                        .path("/api/v1/billing/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri("http://clinic-billing-service:8085"))

                // Auth Service Routes (no JWT filter)
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://clinic-patient-service:8081"))

                // Health check endpoint
                .route("health", r -> r
                        .path("/health")
                        .uri("http://clinic-patient-service:8081"))

                .build();
    }
}

