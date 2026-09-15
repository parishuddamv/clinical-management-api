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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtTokenProvider);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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

        return http.build();
    }
}