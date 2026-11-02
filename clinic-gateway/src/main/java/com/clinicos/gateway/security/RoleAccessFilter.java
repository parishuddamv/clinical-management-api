package com.clinicos.gateway.security;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.RolePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@link RolePolicy} to every clinic API call, for proxied services and the gateway's own
 * endpoints alike. Also refuses tokens of accounts that were suspended or had access removed, since
 * downstream services only check the token's signature.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RoleAccessFilter implements WebFilter {

    static final long CACHE_MS = 30_000;

    private final JwtTokenProvider tokens;
    private final ObjectProvider<ClinicUserRepository> users;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Member> cache = new ConcurrentHashMap<>();

    /** What the filter needs to know about an account; null role/clinic when it does not exist. */
    record Member(String clinicId, String role, boolean invited, boolean active, long at) { }

    public RoleAccessFilter(JwtTokenProvider tokens, ObjectProvider<ClinicUserRepository> users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getRawPath();
        String method = exchange.getRequest().getMethod() == null ? "GET" : exchange.getRequest().getMethod().name();
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if ("OPTIONS".equals(method) || !RolePolicy.governs(path) || authorization == null || !authorization.startsWith("Bearer ")) {
            return chain.filter(exchange); // anonymous calls are rejected by each service's own checks
        }
        String token = authorization.substring(7).trim();
        String email;
        String tokenClinic;
        try {
            if (!Boolean.TRUE.equals(tokens.validateToken(token))) return chain.filter(exchange);
            email = tokens.extractUsername(token);
            tokenClinic = tokens.extractClinicId(token);
        } catch (RuntimeException e) {
            return chain.filter(exchange);
        }
        if (email == null || tokenClinic == null || tokenClinic.isBlank()) return chain.filter(exchange);

        return Mono.fromCallable(() -> lookup(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(member -> {
                    Optional<String> problem = decide(member, tokenClinic, method, path);
                    if (problem.isEmpty()) return chain.filter(exchange);
                    HttpStatus status = problem.get().startsWith("Your access") ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                    return refuse(exchange, status, problem.get());
                });
    }

    /** Empty when allowed; otherwise the message to show. */
    static Optional<String> decide(Member member, String tokenClinic, String method, String path) {
        if (member == null) return Optional.empty(); // service accounts and unknown users: services decide
        if (!member.active() || member.clinicId() == null || !member.clinicId().equals(tokenClinic)) {
            return Optional.of("Your access to this clinic has been removed. Please contact the clinic owner.");
        }
        return RolePolicy.refusal(member.role(), member.invited(), method, path);
    }

    Member lookup(String email) {
        String key = email.toLowerCase(java.util.Locale.ROOT);
        Member hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < CACHE_MS) return hit;
        ClinicUserRepository repo = users.getIfAvailable();
        if (repo == null) return null;
        Member fresh = repo.findByEmailIgnoreCase(key)
                .map(u -> new Member(u.getClinicId(), u.getRole(), u.isInvitedStaff(),
                        u.getStatus() == ClinicUser.UserStatus.APPROVED && Boolean.TRUE.equals(u.getIsActive()), System.currentTimeMillis()))
                .orElse(null);
        if (fresh != null) cache.put(key, fresh);
        return fresh;
    }

    /** Role or access changed: apply it on the next request instead of after the cache expires. */
    public void forget(String email) {
        if (email != null) cache.remove(email.toLowerCase(java.util.Locale.ROOT));
    }

    private Mono<Void> refuse(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = json.writeValueAsBytes(ApiResponse.error(message));
        } catch (Exception e) {
            body = ("{\"success\":false,\"message\":\"" + message.replace("\"", "'") + "\"}").getBytes();
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
