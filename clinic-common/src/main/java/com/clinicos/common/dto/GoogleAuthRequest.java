package com.clinicos.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Google Authentication Request from Frontend
 * Supports both 'idToken' and 'credential' field names for compatibility
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthRequest {
    @JsonAlias({"credential", "id_token", "token"})
    private String idToken;        // Google ID Token from frontend (required)
    private String accessToken;    // Optional: Google Access Token
    private String clinicId;       // Optional: If user already assigned to clinic
    
    /**
     * Get the token, checking both idToken and credential fields
     */
    public String getEffectiveToken() {
        return idToken;
    }
}

