package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInviteRequest {
    private String email;
    private String fullName;
    private String role;
    private String phone;
    private Long staffMemberId;
}
