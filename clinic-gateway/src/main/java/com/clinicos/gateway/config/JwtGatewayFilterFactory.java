package com.clinicos.gateway.config;

import com.clinicos.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter for JWT token validation and multi-tenancy context setting
 */
@Slf4j
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
            String path = exchange.getRequest().getPath().toString();
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            // Debug: Log full auth header info for troubleshooting
            if (authHeader != null) {
                log.info("Auth header length: {}, starts with 'Bearer ': {}, first 50 chars: [{}]", 
                    authHeader.length(),
                    authHeader.startsWith("Bearer "),
                    authHeader.length() > 50 ? authHeader.substring(0, 50) : authHeader);
                
                // Check for common issues
                if (authHeader.equals("Bearer null") || authHeader.equals("Bearer undefined")) {
                    log.error("Frontend is sending literal 'null' or 'undefined' as token!");
                    return onError(exchange, "Unauthorized - Token is null or undefined", HttpStatus.UNAUTHORIZED);
                }
                if (authHeader.startsWith("Bearer [object")) {
                    log.error("Frontend is sending object instead of token string!");
                    return onError(exchange, "Unauthorized - Invalid token format (object instead of string)", HttpStatus.UNAUTHORIZED);
                }
            } else {
                log.warn("No Authorization header present for path: {}", path);
            }
            
            String token = extractToken(exchange);

            if (token == null) {
                log.warn("No JWT token found in Authorization header for path: {}", path);
                return onError(exchange, "Unauthorized - Missing JWT token", HttpStatus.UNAUTHORIZED);
            }
            
            // Check if token looks like a valid JWT (should have 3 parts separated by dots)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("Invalid JWT format: expected 3 parts, got {}. Token starts with: [{}]", 
                    parts.length, 
                    token.length() > 30 ? token.substring(0, 30) : token);
                return onError(exchange, "Unauthorized - Invalid JWT format", HttpStatus.UNAUTHORIZED);
            }
            
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("JWT validation failed for path: {}", path);
                return onError(exchange, "Unauthorized - Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }

            try {
                String username = jwtTokenProvider.extractUsername(token);
                String clinicId = jwtTokenProvider.extractClinicId(token);

                log.debug("Authenticated user: {} with clinic: {} for path: {}", username, clinicId, path);

                // Add clinic_id to request headers for downstream services
                exchange.getRequest().mutate()
                    .header("X-Clinic-ID", clinicId)
                    .header("X-User-ID", username)
                    .build();

                return chain.filter(exchange);
            } catch (Exception e) {
                log.error("Token validation failed for path: {}", path, e);
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
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        // Include helpful debug info in response
        String path = exchange.getRequest().getPath().toString();
        String debugHint = "Ensure your frontend sends 'Authorization: Bearer <token>' header. " +
                           "Token should be stored from /api/v1/auth/google response at data.data.token";
        
        String jsonResponse = String.format(
            "{\"success\":false,\"error\":\"%s\",\"path\":\"%s\",\"hint\":\"%s\"}", 
            message, path, debugHint
        );
        
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(jsonResponse.getBytes()))
        );
    }

    public static class Config {
        // Configuration properties can be added here
    }
}
