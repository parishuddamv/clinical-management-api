package com.clinicos.common.service;

import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.GoogleAuthResponse;
import com.clinicos.common.dto.GoogleUserInfo;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Google OAuth2 Authentication Service
 * Verifies Google ID Tokens, generates JWT tokens, and checks user approval status
 * ClinicUserRepository is optional - only used when available (in API Gateway)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ClinicUserRepository> clinicUserRepositoryProvider;

    @Value("${app.google.clientId:}")
    private String googleClientId;

    // ...existing code...

    /**
     * Verify Google ID Token and authenticate user
     * @param request Google auth request with ID token
     * @return JWT token, user info, and approval status
     */
    public GoogleAuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        try {
            log.info("Authenticating user with Google ID Token");
            
            // Validate request
            if (request == null) {
                throw new IllegalArgumentException("Request body is null");
            }
            
            String idToken = request.getIdToken();
            if (idToken == null || idToken.trim().isEmpty()) {
                log.error("idToken is null or empty. Request received: clinicId={}", request.getClinicId());
                throw new IllegalArgumentException("idToken is required. Please send {\"idToken\": \"<your-google-token>\", \"clinicId\": \"CLINIC_001\"}");
            }

            // Decode Google ID Token
            GoogleUserInfo googleUser = decodeGoogleToken(idToken);
            String normalizedEmail = googleUser.getEmail() == null ? "" : googleUser.getEmail().trim().toLowerCase();
            log.info("Google authentication successful for user: {}", normalizedEmail);

            // Assign clinic (default or from request)
            String clinicId = request.getClinicId() != null ? 
                    request.getClinicId() : 
                    "CLINIC_001";  // Default clinic for now

            // Check if user exists and get approval status (only if repository is available)
            ClinicUserRepository repo = clinicUserRepositoryProvider.getIfAvailable();
            
            boolean isApproved = false;
            String userStatus = "NEW";
            boolean needsDemoBooking = false;
            String statusMessage = "New user - please book a demo";

            if (repo != null) {
                Optional<ClinicUser> existingUser = repo.findByEmail(normalizedEmail);
                if (existingUser.isEmpty()) {
                    existingUser = repo.findByEmailIgnoreCase(normalizedEmail);
                }
                
                if (existingUser.isPresent()) {
                    ClinicUser user = existingUser.get();
                    userStatus = user.getStatus().name();
                    isApproved = user.getStatus() == ClinicUser.UserStatus.APPROVED && user.getIsActive();
                    needsDemoBooking = !isApproved && user.getStatus() != ClinicUser.UserStatus.REJECTED;
                    
                    if (isApproved) {
                        statusMessage = "User approved - welcome back!";
                        clinicId = (user.getClinicId() == null || user.getClinicId().isBlank()) ? "CLINIC_001" : user.getClinicId();
                    } else if (user.getStatus() == ClinicUser.UserStatus.REJECTED) {
                        statusMessage = "Registration rejected: " + user.getRejectionReason();
                    } else if (user.getStatus() == ClinicUser.UserStatus.SUSPENDED) {
                        statusMessage = "Account suspended";
                    } else {
                        statusMessage = "Registration pending approval";
                    }
                    
                    // Update last login
                    user.setLastLogin(java.time.LocalDateTime.now());
                    repo.save(user);
                } else {
                    // New user
                    statusMessage = "New user - please book a demo";
                    needsDemoBooking = true;
                }
            } else {
                // ClinicUserRepository not available (non-gateway service)
                log.debug("ClinicUserRepository not available in this service context - skipping user check");
                needsDemoBooking = true;
            }

            // Generate JWT token with clinic context (even for unapproved users)
            String jwtToken = jwtTokenProvider.generateToken(
                    normalizedEmail,
                    clinicId
            );

            log.info("JWT token generated for user: {} in clinic: {} (Approved: {})", 
                    normalizedEmail, clinicId, isApproved);

            // Build response with approval status
            return GoogleAuthResponse.builder()
                    .token(jwtToken)
                    .user(GoogleAuthResponse.AuthUser.builder()
                            .id(googleUser.getSub())
                            .email(normalizedEmail)
                            .name(googleUser.getName())
                            .picture(googleUser.getPicture())
                            .clinicId(clinicId)
                            .build())
                    .clinicId(clinicId)
                    .expiresIn(86400)  // 24 hours
                    // New fields for approval workflow
                    .userStatus(userStatus)
                    .isApproved(isApproved)
                    .needsDemoBooking(needsDemoBooking)
                    .message(statusMessage)
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

