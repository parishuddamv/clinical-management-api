package com.clinicos.gateway.handler;

import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.RegistrationAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

/** Works out which clinic member is calling a gateway-owned clinic endpoint. Blocking: call off the event loop. */
@Component
@RequiredArgsConstructor
public class ClinicAccess {

    private final JwtTokenProvider tokens;
    private final RegistrationAccess access;

    /** The approved, active member of the clinic named in the caller's token. */
    public ClinicUser member(ServerRequest request) {
        String authorization = request.headers().firstHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RegistrationException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        }
        String token = authorization.substring(7).trim();
        if (!Boolean.TRUE.equals(tokens.validateToken(token))) {
            throw new RegistrationException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        }
        return access.requireClinicAccess(authorization, tokens.extractClinicId(token));
    }

    /** As {@link #member}, but only the clinic's administrator. */
    public ClinicUser admin(ServerRequest request) {
        ClinicUser caller = member(request);
        if (!"ADMIN".equalsIgnoreCase(caller.getRole()) && !Boolean.TRUE.equals(caller.getIsSuperAdmin())) {
            throw new RegistrationException(HttpStatus.FORBIDDEN, "Only the clinic administrator can do this.");
        }
        return caller;
    }
}
