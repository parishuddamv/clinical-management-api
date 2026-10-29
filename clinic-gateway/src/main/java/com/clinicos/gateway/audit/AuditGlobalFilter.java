package com.clinicos.gateway.audit;

import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.service.AuditService;
import com.clinicos.common.service.AuditTrail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Records who viewed or changed which clinic record, after the response is decided.
 * Written in the background, so a slow audit insert never delays the clinic's request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider tokens;
    private final AuditService audit;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() == null ? null : request.getMethod().name();
        String path = request.getURI().getRawPath();
        return chain.filter(exchange).doFinally(signal -> {
            HttpStatusCode code = exchange.getResponse().getStatusCode();
            int status = code == null ? 0 : code.value();
            AuditTrail.describe(method, path, status).ifPresent(event -> {
                String[] who = identity(request.getHeaders().getFirst("Authorization"));
                if (who == null) return;
                String ip = clientIp(request);
                Mono.fromRunnable(() -> audit.record(event, who[1], who[0], ip))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> Mono.empty())
                        .subscribe();
            });
        });
    }

    /** [user, clinic] from a valid token, or null. */
    private String[] identity(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        try {
            String token = authorization.substring(7).trim();
            if (!Boolean.TRUE.equals(tokens.validateToken(token))) return null;
            String user = tokens.extractUsername(token);
            String clinic = tokens.extractClinicId(token);
            return user == null || clinic == null || clinic.isBlank() ? null : new String[]{user, clinic};
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddress() == null || request.getRemoteAddress().getAddress() == null
                ? null : request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
