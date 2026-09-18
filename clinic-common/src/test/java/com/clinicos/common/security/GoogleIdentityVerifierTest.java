package com.clinicos.common.security;

import com.clinicos.common.exception.RegistrationException;
import com.google.auth.oauth2.TokenVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class GoogleIdentityVerifierTest {
    private KeyPair keys;
    private GoogleIdentityVerifier verifier;

    @BeforeEach void setup() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        verifier = new GoogleIdentityVerifier("client-id", TokenVerifier.newBuilder()
                .setAudience("client-id").setPublicKey(keys.getPublic()).build());
    }

    private String token(String issuer, String audience, boolean verified, int seconds) {
        return Jwts.builder().setIssuer(issuer).setAudience(audience).setSubject("google-id")
                .claim("email", "root@example.com").claim("email_verified", verified)
                .setExpiration(Date.from(Instant.now().plusSeconds(seconds)))
                .signWith(keys.getPrivate(), SignatureAlgorithm.RS256).compact();
    }

    @ParameterizedTest
    @ValueSource(strings = {"accounts.google.com", "https://accounts.google.com"})
    void acceptsVerifiedGoogleIdentity(String issuer) {
        assertThat(verifier.verify(token(issuer, "client-id", true, 300)).getEmail()).isEqualTo("root@example.com");
    }

    @Test void rejectsWrongIssuerAudienceExpiredOrUnverifiedEmail() {
        for (String token : new String[] {
                token("https://evil.example", "client-id", true, 300),
                token("accounts.google.com", "other-client", true, 300),
                token("accounts.google.com", "client-id", false, 300),
                token("accounts.google.com", "client-id", true, -300)}) {
            assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(RegistrationException.class)
                    .hasMessage("Invalid or expired Google ID token.");
        }
    }

    @Test void rejectsForgedSignatureAndUnsignedToken() throws Exception {
        String valid = token("accounts.google.com", "client-id", true, 300);
        var other = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        var rejecting = new GoogleIdentityVerifier("client-id", TokenVerifier.newBuilder()
                .setAudience("client-id").setPublicKey(other.getPublic()).build());
        assertThatThrownBy(() -> rejecting.verify(valid)).isInstanceOf(RegistrationException.class);
        assertThatThrownBy(() -> verifier.verify("eyJhbGciOiJub25lIn0.e30."))
                .isInstanceOf(RegistrationException.class);
    }

    @Test void failsClosedWithoutConfiguredClientId() {
        assertThatThrownBy(() -> new GoogleIdentityVerifier("").verify("ignored"))
                .isInstanceOf(RegistrationException.class).hasMessage("Google authentication is not configured.");
    }
}
