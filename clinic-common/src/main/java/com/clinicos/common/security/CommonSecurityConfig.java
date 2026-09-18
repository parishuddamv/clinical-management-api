package com.clinicos.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration with JWT authentication.
 * Configures stateless session and JWT filter.
 *
 * CORS is handled centrally by the API Gateway.
 */
@Configuration
@ConditionalOnProperty(
        name = "app.security.common-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class CommonSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RegistrationAccess registrationAccess;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtTokenProvider);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, com.fasterxml.jackson.databind.ObjectMapper objectMapper) throws Exception {
        http
                // Disable CSRF for stateless API
                .csrf(csrf -> csrf.disable())

                // CORS is handled centrally by the API Gateway
                .cors(cors -> cors.disable())

                // Set session management to stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authorization
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/admin/**").access((authentication, context) -> {
                            boolean allowed;
                            try {
                                registrationAccess.requireSuperAdmin(context.getRequest().getHeader("Authorization"));
                                allowed = true;
                            } catch (RuntimeException e) {
                                allowed = false;
                            }
                            return new org.springframework.security.authorization.AuthorizationDecision(allowed);
                        })
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/health",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Add JWT filter
                .addFilterBefore(
                        jwtAuthFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(request.getRequestURI().startsWith("/api/v1/auth/admin/") ? 403 : 401);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), com.clinicos.common.dto.ApiResponse.error("Authorization is required."));
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), com.clinicos.common.dto.ApiResponse.error("Access denied."));
                }));
        return http.build();
    }
}