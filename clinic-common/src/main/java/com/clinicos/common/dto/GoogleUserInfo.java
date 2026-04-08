package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Google User Information from OAuth2 Token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleUserInfo {
    private String sub;                  // Google User ID (unique)
    private String email;                // User email
    private String name;                 // Full name
    private String picture;              // Profile picture URL
    private String givenName;            // First name
    private String familyName;           // Last name
    private boolean emailVerified;       // Email verified status
    private String locale;               // User locale

    /**
     * Parse from Google ID Token claims (Map)
     */
    public static GoogleUserInfo fromTokenClaims(Map<String, Object> claims) {
        return GoogleUserInfo.builder()
                .sub((String) claims.get("sub"))
                .email((String) claims.get("email"))
                .name((String) claims.get("name"))
                .picture((String) claims.get("picture"))
                .givenName((String) claims.get("given_name"))
                .familyName((String) claims.get("family_name"))
                .emailVerified((Boolean) claims.getOrDefault("email_verified", false))
                .locale((String) claims.get("locale"))
                .build();
    }
}

