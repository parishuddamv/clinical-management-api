package com.clinicos.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the API Gateway (Spring Cloud Gateway / WebFlux).
 * This applies CORS headers to ALL routes passing through the gateway.
 * 
 * Handles:
 * - Preflight (OPTIONS) requests
 * - Actual requests with proper CORS headers
 * - Credentials (cookies, authorization headers)
 * - Multiple allowed origins
 */
@Configuration
public class CorsConfig {

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:4200,http://localhost:43263}")
    private String allowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH,HEAD}")
    private String allowedMethods;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Parse comma-separated origins from environment variable
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        
        // Use allowedOriginPatterns when allowCredentials is true
        // This is more flexible and avoids the "*" with credentials issue
        origins.forEach(origin -> corsConfiguration.addAllowedOriginPattern(origin.trim()));

        // Allowed HTTP methods for preflight requests
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Allowed request headers - allow all
        corsConfiguration.setAllowedHeaders(List.of("*"));

        // Exposed headers (headers client can read)
        corsConfiguration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // Allow credentials (cookies, authorization headers)
        corsConfiguration.setAllowCredentials(true);

        // Cache preflight responses for 1 hour (3600 seconds)
        corsConfiguration.setMaxAge(3600L);

        // Note: reactive gateway uses org.springframework.web.cors.reactive classes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS configuration to all paths
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }
}
