package com.clinicos.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration for microservice routing
 * URLs are configurable via environment variables for different environments:
 * - Docker: uses container names (clinic-patient-service)
 * - Local: uses localhost URLs
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtGatewayFilterFactory jwtGatewayFilterFactory;

    @Value("${service.patient.url:http://localhost:8081}")
    private String patientServiceUrl;

    @Value("${service.appointment.url:http://localhost:8082}")
    private String appointmentServiceUrl;

    @Value("${service.followup.url:http://localhost:8083}")
    private String followupServiceUrl;

    @Value("${service.notification.url:http://localhost:8084}")
    private String notificationServiceUrl;

    @Value("${service.billing.url:http://localhost:8085}")
    private String billingServiceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        log.info("Configuring gateway routes:");
        log.info("  Patient Service: {}", patientServiceUrl);
        log.info("  Appointment Service: {}", appointmentServiceUrl);
        log.info("  Follow-up Service: {}", followupServiceUrl);
        log.info("  Notification Service: {}", notificationServiceUrl);
        log.info("  Billing Service: {}", billingServiceUrl);

        return builder.routes()
                // Patient Service Routes
                .route("patient-service", r -> r
                        .path("/api/v1/patients/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(patientServiceUrl))

                // Appointment Service Routes
                .route("appointment-service", r -> r
                        .path("/api/v1/appointments/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(appointmentServiceUrl))

                // Follow-up Service Routes
                .route("followup-service", r -> r
                        .path("/api/v1/followups/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(followupServiceUrl))

                // Notification Service Routes
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(notificationServiceUrl))

                // Billing Service Routes
                .route("billing-service", r -> r
                        .path("/api/v1/billing/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(billingServiceUrl))

                // Auth Service Routes (no JWT filter)
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri(patientServiceUrl))

                // Health check endpoint
                .route("health", r -> r
                        .path("/health")
                        .uri(patientServiceUrl))

                .build();
    }
}

