package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Google Authentication Request from Frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthRequest {
    private String idToken;        // Google ID Token from frontend (required)
    private String accessToken;    // Optional: Google Access Token
    private String clinicId;       // Optional: If user already assigned to clinic
}

