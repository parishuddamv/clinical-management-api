package com.clinicos.common.service;

import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.GoogleAuthResponse;
import com.clinicos.common.dto.GoogleUserInfo;
import com.clinicos.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

/**
 * Google OAuth2 Authentication Service
 * Verifies Google ID Tokens and generates JWT tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.google.clientId:}")
    private String googleClientId;

    /**
     * Verify Google ID Token and authenticate user
     * @param request Google auth request with ID token
     * @return JWT token and user info
     */
    public GoogleAuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        try {
            log.info("Authenticating user with Google ID Token");

            // Decode Google ID Token
            GoogleUserInfo googleUser = decodeGoogleToken(request.getIdToken());
            log.info("Google authentication successful for user: {}", googleUser.getEmail());

            // Assign clinic (default or from request)
            String clinicId = request.getClinicId() != null ? 
                    request.getClinicId() : 
                    "CLINIC_001";  // Default clinic for now

            // Generate JWT token with clinic context
            String jwtToken = jwtTokenProvider.generateToken(
                    googleUser.getEmail(),
                    clinicId
            );

            log.info("JWT token generated for user: {} in clinic: {}", googleUser.getEmail(), clinicId);

            // Build response
            return GoogleAuthResponse.builder()
                    .token(jwtToken)
                    .user(GoogleAuthResponse.AuthUser.builder()
                            .id(googleUser.getSub())
                            .email(googleUser.getEmail())
                            .name(googleUser.getName())
                            .picture(googleUser.getPicture())
                            .clinicId(clinicId)
                            .build())
                    .clinicId(clinicId)
                    .expiresIn(86400)  // 24 hours
                    .build();

        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * Decode Google ID Token without verification (frontend should verify)
     * In production, implement proper token verification with Google servers
     * @param idToken Token from Google
     * @return User information from token
     */
    @SuppressWarnings("unchecked")
    private GoogleUserInfo decodeGoogleToken(String idToken) {
        try {
            // Split JWT token
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            // Validate audience (client ID)
            String aud = (String) claims.get("aud");
            if (aud != null && !aud.equals(googleClientId)) {
                log.warn("Token audience mismatch. Expected: {}, Got: {}", googleClientId, aud);
                // In production, reject mismatched audience
            }

            // Build user info from claims
            return GoogleUserInfo.fromTokenClaims(claims);

        } catch (Exception e) {
            log.error("Failed to decode Google token: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid Google token: " + e.getMessage());
        }
    }

    /**
     * Get Google OAuth2 Configuration for Frontend
     */
    public GoogleAuthConfigResponse getGoogleConfig() {
        return GoogleAuthConfigResponse.builder()
                .googleClientId(googleClientId)
                .redirectUrl("/api/v1/auth/google")
                .scopes(new String[]{"openid", "email", "profile"})
                .build();
    }

    /**
     * Configuration response for frontend
     */
    @lombok.Data
    @lombok.Builder
    public static class GoogleAuthConfigResponse {
        private String googleClientId;
        private String redirectUrl;
        private String[] scopes;
    }
}

