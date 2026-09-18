package com.clinicos.common.service;

import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.GoogleAuthResponse;
import com.clinicos.common.dto.GoogleUserInfo;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.GoogleIdentityVerifier;
import com.clinicos.common.exception.RegistrationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    private final GoogleIdentityVerifier identityVerifier;

    @Value("${app.google.clientId:}")
    private String googleClientId;

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
                throw new IllegalArgumentException("idToken is required.");
            }

            // Verify Google signature and identity claims before issuing an application JWT.
            GoogleUserInfo googleUser = identityVerifier.verify(idToken);
            String normalizedEmail = googleUser.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
            log.info("Google authentication successful for user: {}", normalizedEmail);

            // Only an approved server-side association may supply clinic context.
            String clinicId = null; // Browser-supplied clinic associations are never trusted.

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
                    isApproved = user.getStatus() == ClinicUser.UserStatus.APPROVED && Boolean.TRUE.equals(user.getIsActive());
                    needsDemoBooking = !isApproved && user.getStatus() != ClinicUser.UserStatus.REJECTED;
                    
                    if (isApproved) {
                        statusMessage = "User approved - welcome back!";
                        clinicId = user.getClinicId();
                    } else if (user.getStatus() == ClinicUser.UserStatus.REJECTED) {
                        statusMessage = "Registration rejected: " + user.getRejectionReason();
                    } else if (user.getStatus() == ClinicUser.UserStatus.SUSPENDED) {
                        statusMessage = "Account suspended";
                    } else {
                        statusMessage = "Registration pending approval";
                    }
                    
                    // Update last login
                    repo.recordLogin(user.getId(), java.time.LocalDateTime.now());
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

            // Unapproved users receive identity-only tokens for their own registration status.
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

        } catch (RegistrationException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage(), e);
            throw new RegistrationException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Google authentication failed. Please try again later.");
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

