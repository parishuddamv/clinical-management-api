package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordLoginRequest {
    private String email;
    @ToString.Exclude
    private String password;
}
