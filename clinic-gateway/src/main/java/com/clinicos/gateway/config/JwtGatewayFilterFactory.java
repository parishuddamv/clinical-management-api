package com.clinicos.gateway.config;

import com.clinicos.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter for JWT token validation and multi-tenancy context setting
 */
@Component
public class JwtGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtGatewayFilterFactory.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtGatewayFilterFactory(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange);

            if (token == null || !jwtTokenProvider.validateToken(token)) {
                return onError(exchange, "Unauthorized - Invalid or missing JWT token", HttpStatus.UNAUTHORIZED);
            }

            try {
                String username = jwtTokenProvider.extractUsername(token);
                String clinicId = jwtTokenProvider.extractClinicId(token);

                // Add clinic_id to response headers for downstream services
                exchange.getResponse().getHeaders().add("X-Clinic-ID", clinicId);
                exchange.getResponse().getHeaders().add("X-User-ID", username);

                return chain.filter(exchange);
            } catch (Exception e) {
                return onError(exchange, "Unauthorized - Token validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private String extractToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes()))
        );
    }

    public static class Config {
        // Configuration properties can be added here
    }
}

