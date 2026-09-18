package com.clinicos.common.security;

import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Shared by MVC and WebFlux. Never relies on browser roles or thread-local reactive context. */
@Service
@RequiredArgsConstructor
public class RegistrationAccess {
    private final JwtTokenProvider tokens;
    private final ObjectProvider<ClinicUserRepository> users;

    public String identity(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw denied();
        }
        String token = authorization.substring(7).trim();
        if (!tokens.validateToken(token)) {
            throw denied();
        }
        String email = tokens.extractUsername(token);
        if (email == null || email.isBlank()) {
            throw denied();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public ClinicUser requireSuperAdmin(String authorization) {
        ClinicUser caller = users.getObject().findByEmailIgnoreCase(identity(authorization))
                .orElseThrow(RegistrationAccess::denied);
        if (!isSuperAdmin(caller)) {
            throw denied();
        }
        return caller;
    }

    public void requireSelfOrSuperAdmin(String authorization, String email) {
        if (!identity(authorization).equalsIgnoreCase(email.trim())) {
            requireSuperAdmin(authorization);
        }
    }

    public ClinicUser requireClinicAccess(String authorization, String clinicId) {
        ClinicUser caller = users.getObject().findByEmailIgnoreCase(identity(authorization))
                .orElseThrow(RegistrationAccess::denied);
        if (caller.getStatus() != ClinicUser.UserStatus.APPROVED || !Boolean.TRUE.equals(caller.getIsActive())
                || clinicId == null || clinicId.isBlank() || !clinicId.equals(caller.getClinicId())) {
            throw new RegistrationException(HttpStatus.FORBIDDEN, "Approved, active clinic membership is required.");
        }
        return caller;
    }

    private boolean isSuperAdmin(ClinicUser caller) {
        return caller.getStatus() == ClinicUser.UserStatus.APPROVED
                && Boolean.TRUE.equals(caller.getIsActive())
                // role was historically supplied by public registration; only this server-managed flag is authoritative.
                && Boolean.TRUE.equals(caller.getIsSuperAdmin());
    }

    private static RegistrationException denied() {
        return new RegistrationException(HttpStatus.FORBIDDEN, "Super Admin authorization or verified account ownership is required.");
    }
}
