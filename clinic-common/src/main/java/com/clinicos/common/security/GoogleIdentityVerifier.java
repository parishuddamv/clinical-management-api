package com.clinicos.common.security;

import com.clinicos.common.dto.GoogleUserInfo;
import com.clinicos.common.exception.RegistrationException;
import com.google.auth.oauth2.TokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Google signatures must be verified before exchanging an ID token for an application JWT. */
@Component
public class GoogleIdentityVerifier {
    private final String clientId;
    private final TokenVerifier verifier;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleIdentityVerifier(@Value("${app.google.clientId:}") String clientId) {
        this(clientId, TokenVerifier.newBuilder().setAudience(clientId)
                .setCertificatesLocation("https://www.googleapis.com/oauth2/v3/certs").build());
    }

    GoogleIdentityVerifier(String clientId, TokenVerifier verifier) {
        this.clientId = clientId;
        this.verifier = verifier;
    }

    public GoogleUserInfo verify(String token) {
        if (clientId == null || clientId.isBlank()) {
            throw new RegistrationException(HttpStatus.SERVICE_UNAVAILABLE, "Google authentication is not configured.");
        }
        try {
            var verified = verifier.verify(token);
            var claims = verified.getPayload();
            if (!"RS256".equals(verified.getHeader().getAlgorithm())
                    || !("https://accounts.google.com".equals(claims.getIssuer()) || "accounts.google.com".equals(claims.getIssuer()))
                    || claims.getExpirationTimeSeconds() == null
                    || claims.getExpirationTimeSeconds() <= java.time.Instant.now().getEpochSecond()
                    || !Boolean.TRUE.equals(claims.get("email_verified"))
                    || !(claims.get("email") instanceof String email) || email.isBlank()
                    || claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw new IllegalArgumentException("Invalid identity claims");
            }
            return GoogleUserInfo.fromTokenClaims(claims);
        } catch (Exception e) {
            throw new RegistrationException(HttpStatus.UNAUTHORIZED, "Invalid or expired Google ID token.");
        }
    }
}
