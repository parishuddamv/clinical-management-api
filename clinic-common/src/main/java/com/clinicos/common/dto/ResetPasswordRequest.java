package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @ToString.Exclude
    private String token;
    @ToString.Exclude
    private String password;
    @ToString.Exclude
    private String confirmPassword;
}
