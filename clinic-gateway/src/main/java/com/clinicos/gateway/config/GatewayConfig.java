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

    @Value("${service.emr.url:http://localhost:8086}")
    private String emrServiceUrl;

    @Value("${service.staff.url:http://localhost:8087}")
    private String staffServiceUrl;

    @Value("${service.feedback.url:http://localhost:8088}")
    private String feedbackServiceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        log.info("Configuring gateway routes:");
        log.info("  Patient Service: {}", patientServiceUrl);
        log.info("  Appointment Service: {}", appointmentServiceUrl);
        log.info("  Follow-up Service: {}", followupServiceUrl);
        log.info("  Notification Service: {}", notificationServiceUrl);
        log.info("  Billing Service: {}", billingServiceUrl);
        log.info("  EMR Service: {}", emrServiceUrl);
        log.info("  Staff Service: {}", staffServiceUrl);
        log.info("  Feedback Service: {}", feedbackServiceUrl);

        return builder.routes()
                // Authentication Service Routes handled by AuthHandler (via RouterFunction)
                // See AuthRouterConfig.java for auth endpoint definitions

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

                // Subscription Commerce Routes
                .route("subscription-service", r -> r
                        .path("/api/v1/subscriptions/**", "/api/v1/platform/subscriptions/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(billingServiceUrl))

                // EMR Service Routes (Electronic Medical Records & E-Prescription)
                .route("emr-service", r -> r
                        .path("/api/v1/emr/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(emrServiceUrl))

                // Staff Service Routes (Multi-Doctor/Staff Management)
                .route("staff-service", r -> r
                        .path("/api/v1/staff/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(staffServiceUrl))

                // Feedback Service Routes (Patient Reviews & Feedback)
                .route("feedback-service", r -> r
                        .path("/api/v1/feedback/**")
                        .filters(f -> f.filter(jwtGatewayFilterFactory.apply(new JwtGatewayFilterFactory.Config())))
                        .uri(feedbackServiceUrl))

                // Public feedback submission by token (no JWT filter)
                .route("feedback-public", r -> r
                        .path("/api/v1/feedback/token/**")
                        .uri(feedbackServiceUrl))


                .build();
    }
}
