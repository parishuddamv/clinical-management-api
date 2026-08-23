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
 * Gateway filter for JWT token validation and multi-tenancy context setting.
 */
@Slf4j
@Component
public class JwtGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtGatewayFilterFactory.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtGatewayFilterFactory(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            String path = exchange.getRequest().getPath().toString();

            String authHeader =
                    exchange.getRequest()
                            .getHeaders()
                            .getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {

                log.warn(
                        "Missing/invalid Authorization header for path: {}",
                        path
                );

                return onError(
                        exchange,
                        "Unauthorized - Missing JWT token",
                        HttpStatus.UNAUTHORIZED
                );
            }

            String token = authHeader.substring(7).trim();

            if (token.isEmpty()) {

                return onError(
                        exchange,
                        "Unauthorized - Empty JWT token",
                        HttpStatus.UNAUTHORIZED
                );
            }

            String[] parts = token.split("\\.");

            if (parts.length != 3) {

                log.warn(
                        "Invalid JWT format for path: {}",
                        path
                );

                return onError(
                        exchange,
                        "Unauthorized - Invalid JWT format",
                        HttpStatus.UNAUTHORIZED
                );
            }

            if (!jwtTokenProvider.validateToken(token)) {

                log.warn(
                        "JWT validation failed for path: {}",
                        path
                );

                return onError(
                        exchange,
                        "Unauthorized - Invalid JWT token",
                        HttpStatus.UNAUTHORIZED
                );
            }

            try {

                String username =
                        jwtTokenProvider.extractUsername(token);

                String clinicId =
                        jwtTokenProvider.extractClinicId(token);

                if (username == null || username.isBlank()) {

                    return onError(
                            exchange,
                            "Unauthorized - Missing user identity",
                            HttpStatus.UNAUTHORIZED
                    );
                }

                if (clinicId == null || clinicId.isBlank()) {

                    return onError(
                            exchange,
                            "Unauthorized - Missing clinic context",
                            HttpStatus.UNAUTHORIZED
                    );
                }

                log.debug(
                        "Authenticated request: user={}, clinic={}, path={}",
                        username,
                        clinicId,
                        path
                );

                ServerWebExchange mutatedExchange =
                        exchange.mutate()
                                .request(
                                        exchange.getRequest()
                                                .mutate()
                                                .header("X-Clinic-ID", clinicId)
                                                .header("X-User-ID", username)
                                                .build()
                                )
                                .build();

                return chain.filter(mutatedExchange);

            } catch (Exception e) {

                log.error(
                        "JWT processing failed for path: {}",
                        path,
                        e
                );

                return onError(
                        exchange,
                        "Unauthorized - Token processing failed",
                        HttpStatus.UNAUTHORIZED
                );
            }
        };
    }

    private Mono<Void> onError(
            ServerWebExchange exchange,
            String message,
            HttpStatus status) {

        exchange.getResponse().setStatusCode(status);

        exchange.getResponse()
                .getHeaders()
                .add("Content-Type", "application/json");

        String path =
                exchange.getRequest()
                        .getPath()
                        .toString();

        String jsonResponse = String.format(
                "{\"success\":false,\"error\":\"%s\",\"path\":\"%s\"}",
                message,
                path
        );

        return exchange.getResponse()
                .writeWith(
                        Mono.just(
                                exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(jsonResponse.getBytes())
                        )
                );
    }

    public static class Config {
    }
}