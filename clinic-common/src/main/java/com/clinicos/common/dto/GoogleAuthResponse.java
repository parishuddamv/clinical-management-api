package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Google Authentication Response to Frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthResponse {
    private String token;              // JWT Token for API authentication
    private String refreshToken;       // Optional: Refresh token for extending session
    private AuthUser user;             // Authenticated user details
    private long expiresIn;            // Token expiration in seconds (86400 = 24 hours)
    private String clinicId;           // Clinic ID for the user

    /**
     * Nested User Information in Response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthUser {
        private String id;             // Google User ID
        private String email;          // User email
        private String name;           // Full name
        private String picture;        // Profile picture URL
        private String clinicId;       // Assigned clinic
    }
}

